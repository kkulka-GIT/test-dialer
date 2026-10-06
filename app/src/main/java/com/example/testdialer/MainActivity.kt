package com.example.testdialer

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.res.ColorStateList
import android.widget.ProgressBar
import androidx.core.view.WindowInsetsCompat
import com.example.testdialer.report.RunReportFormatter
import com.example.testdialer.report.RunReportFiles
import java.util.concurrent.Executors
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.telephony.TelephonyManager
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.lifecycle.ViewModelProvider
import androidx.core.view.ViewCompat
import com.example.testdialer.data.CellularDataInput
import com.example.testdialer.data.CellularDataUiState
import com.example.testdialer.data.CellularDataViewModel
import com.example.testdialer.domain.RunId
import com.example.testdialer.domain.EventId
import com.example.testdialer.domain.StepId
import com.example.testdialer.domain.TestAction
import com.example.testdialer.active.ActiveRunUiState
import com.example.testdialer.active.ActiveRunViewModel
import com.example.testdialer.active.ActiveTaskStatus
import com.example.testdialer.active.LocalScenarioCatalog
import com.example.testdialer.domain.ServiceType
import com.example.testdialer.domain.TestRunStatus
import com.example.testdialer.persistence.TestRunSummary
import com.example.testdialer.persistence.StoredTestRun
import com.example.testdialer.domain.TestEvent
import com.example.testdialer.domain.ObservationStatus
import com.example.testdialer.register.RegisterUiState
import com.example.testdialer.register.RegisterViewModel
import com.example.testdialer.session.ManualSessionUiState
import com.example.testdialer.session.ManualSessionViewModel
import com.example.testdialer.sms.GuidedSmsInput
import com.example.testdialer.sms.GuidedSmsOutcome
import com.example.testdialer.sms.GuidedSmsUiState
import com.example.testdialer.sms.GuidedSmsViewModel
import com.example.testdialer.sms.SmsComposerIntentFactory
import com.example.testdialer.domain.execution.TimelineEntryKind
import com.example.testdialer.ui.AppSection
import com.example.testdialer.ui.RunHomeView
import com.example.testdialer.ui.SystemStatusStripView
import com.example.testdialer.ui.TestType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MainActivity : ComponentActivity() {
    private val speech by lazy { com.example.testdialer.accessibility.SpeechAnnouncements(applicationContext).apply {
        onUnavailable = { if (!isDestroyed && !isFinishing && lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) Toast.makeText(this@MainActivity, "Polski głos TTS jest niedostępny. Sprawdź ustawienia syntezy mowy telefonu.", Toast.LENGTH_LONG).show() }
    } }
    private var executionFocused = false
    private var lastSpokenKey: String? = null
    private val reportExecutor = Executors.newSingleThreadExecutor()
    private val templates by lazy { com.example.testdialer.templates.TestTemplateStore(applicationContext) }
    private val scenarioPlans by lazy { com.example.testdialer.active.ScenarioPlanStore(applicationContext) }
    private val exportPlansLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) reportExecutor.execute {
            val result = runCatching {
                val archive = com.example.testdialer.active.ScenarioPlanArchive.encode(scenarioPlans.list())
                contentResolver.openOutputStream(uri, "wt")?.use { it.write(archive.toByteArray(Charsets.UTF_8)) }
                    ?: error("Nie można otworzyć pliku do zapisu.")
            }
            templateTransferMessage(if (result.isSuccess) "Kopia planów zapisana" else "Nie udało się zapisać kopii planów: ${result.exceptionOrNull()?.message}")
        }
    }
    private val importPlansLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) reportExecutor.execute {
            val result = runCatching {
                contentResolver.openInputStream(uri)?.use(com.example.testdialer.active.ScenarioPlanArchive::read)
                    ?: error("Nie można otworzyć pliku.")
            }
            runOnUiThread { if (!isDestroyed && !isFinishing) result.fold(::confirmPlanImport) {
                templateTransferMessage("Nie udało się wczytać planów: ${it.message}")
            } }
        }
    }
    private val exportHistoryLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) reportExecutor.execute {
            val result = runCatching {
                val repository = (application as TestDialerApplication).testRunRepository
                val entries = repository.listSummaries().map { summary ->
                    val stored = repository.get(summary.runId) ?: error("Sesja ${summary.runId.value} jest niedostępna.")
                    val reviews = stored.run.events.associate { it.id to billingReviews.get(it.id) }
                        .filterValues { it.reviewedAtMillis > 0 }
                    com.example.testdialer.report.HistoryArchiveEntry(
                        stored, runNotes.get(stored.run.id), reviews, billingReviews.interruptedAt(stored.run.id),
                    )
                }
                val archive = com.example.testdialer.report.HistoryArchive.encode(entries)
                contentResolver.openOutputStream(uri, "wt")?.use { it.write(archive.toByteArray(Charsets.UTF_8)) }
                    ?: error("Nie można otworzyć pliku do zapisu.")
                entries.size
            }
            templateTransferMessage(result.fold({ "Kopia historii zapisana. Sesje: $it." }, { "Nie udało się zapisać historii: ${it.message}" }))
        }
    }
    private val previewHistoryLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) reportExecutor.execute {
            val result = runCatching {
                contentResolver.openInputStream(uri)?.use(com.example.testdialer.report.HistoryArchive::read)
                    ?: error("Nie można otworzyć pliku.")
            }
            runOnUiThread { if (!isDestroyed && !isFinishing) result.fold(::showHistoryImportPreview) {
                templateTransferMessage("Nie udało się odczytać kopii historii: ${it.message}")
            } }
        }
    }
    private val exportTemplatesLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) reportExecutor.execute {
            val result = runCatching {
                val archive = com.example.testdialer.templates.TestTemplateArchive.encode(templates.list())
                contentResolver.openOutputStream(uri, "wt")?.use { it.write(archive.toByteArray(Charsets.UTF_8)) }
                    ?: error("Nie można otworzyć pliku do zapisu.")
            }
            templateTransferMessage(if (result.isSuccess) "Kopia szablonów zapisana" else "Nie udało się zapisać kopii: ${result.exceptionOrNull()?.message}")
        }
    }
    private val importTemplatesLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) reportExecutor.execute {
            val result = runCatching {
                contentResolver.openInputStream(uri)?.use(com.example.testdialer.templates.TestTemplateArchive::read)
                    ?: error("Nie można otworzyć pliku.")
            }
            runOnUiThread {
                if (!isDestroyed && !isFinishing) result.fold(::confirmTemplateImport) {
                    templateTransferMessage("Nie udało się wczytać szablonów: ${it.message}")
                }
            }
        }
    }
    private val billingReviews by lazy { com.example.testdialer.review.BillingReviewStore(applicationContext, (application as TestDialerApplication).annotationStore) }
    private var pendingTemplateId: String? = null
    private var pendingAdditionalType: TestType? = null
    private lateinit var registerFilterLabel: Button
    private lateinit var registerFiltersHost: LinearLayout
    private var registerPage = 0
    private val registerPageSize = 20
    private var registerFilter = com.example.testdialer.register.RegisterFilter()
    private var reportBusy = false
    private val runNotes by lazy { com.example.testdialer.notes.RunNotesStore(applicationContext, (application as TestDialerApplication).annotationStore) }
    private var dataAmountDraft = "1"
    private var dataUnitDraft = "MB"
    private var runNameDraft = ""
    private val sectionButtons = mutableMapOf<AppSection, Button>()
    private val testTypeButtons = mutableMapOf<TestType, Button>()
    private lateinit var contentHost: FrameLayout
    private lateinit var testSection: View
    private lateinit var registerSection: View
    private lateinit var registerListHost: LinearLayout
    private lateinit var manualSessionHost: LinearLayout
    private lateinit var systemStatusStrip: SystemStatusStripView
    private lateinit var runHomeView: RunHomeView
    private lateinit var testScenarioHost: LinearLayout
    private lateinit var voiceStatusText: TextView
    private lateinit var voicePhoneInput: EditText
    private lateinit var voiceNameInput: EditText
    private var networkCallbackRegistered = false
    private var currentSection = AppSection.TEST
    private var currentTestType = TestType.VOICE
    private lateinit var voiceResultStore: VoiceResultStore
    private lateinit var manualSessionViewModel: ManualSessionViewModel
    private lateinit var guidedSmsViewModel: GuidedSmsViewModel
    private lateinit var cellularDataViewModel: CellularDataViewModel
    private lateinit var activeRunViewModel: ActiveRunViewModel
    private lateinit var registerViewModel: RegisterViewModel
    private var manualSessionState = ManualSessionUiState()
    private var guidedSmsState = GuidedSmsUiState()
    private var cellularDataState = CellularDataUiState()
    private var activeRunState = ActiveRunUiState()
    private var registerState = RegisterUiState()
    private var selectedActiveTaskId: StepId? = null
    private var pendingPhoneNumber: String? = null
    private var pendingTestName: String? = null
    private var dialerWasOpened = false
    private var awaitingVoiceOutcome = false
    private var resultSaved = false
    private var voiceNameDraft: String? = null
    private var voicePhoneDraft: String? = null
    private var smsLabelDraft: String? = null
    private var smsDestinationDraft: String? = null
    private var smsMessageDraft: String? = null
    private var dataLabelDraft: String? = null
    private var dataUrlDraft: String? = null

    private val connectivityManager by lazy {
        getSystemService(ConnectivityManager::class.java)
    }

    private val telephonyManager by lazy {
        getSystemService(TelephonyManager::class.java)
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: android.net.Network) {
            runOnUiThread { refreshVoiceStatusBar() }
        }

        override fun onLost(network: android.net.Network) {
            runOnUiThread { refreshVoiceStatusBar() }
        }

        override fun onCapabilitiesChanged(network: android.net.Network, networkCapabilities: NetworkCapabilities) {
            runOnUiThread { refreshVoiceStatusBar() }
        }
    }

    override fun attachBaseContext(newBase: android.content.Context) {
        val mode = newBase.getSharedPreferences("ui-settings", MODE_PRIVATE).getInt("theme", 0)
        val themed = if (mode == 0) newBase else {
            val config = android.content.res.Configuration(newBase.resources.configuration)
            config.uiMode = (config.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK.inv()) or
                if (mode == 2) android.content.res.Configuration.UI_MODE_NIGHT_YES else android.content.res.Configuration.UI_MODE_NIGHT_NO
            newBase.createConfigurationContext(config)
        }
        super.attachBaseContext(themed)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        voiceResultStore = VoiceResultStore(this)
        val repository = (application as TestDialerApplication).testRunRepository
        registerViewModel = ViewModelProvider(
            this,
            RegisterViewModel.Factory(repository),
        )[RegisterViewModel::class.java]
        manualSessionViewModel = ViewModelProvider(
            this,
            ManualSessionViewModel.Factory(repository),
        )[ManualSessionViewModel::class.java]
        guidedSmsViewModel = ViewModelProvider(
            this,
            GuidedSmsViewModel.Factory(repository),
        )[GuidedSmsViewModel::class.java]
        cellularDataViewModel = ViewModelProvider(
            this,
            CellularDataViewModel.Factory(repository, connectivityManager),
        )[CellularDataViewModel::class.java]
        activeRunViewModel = ViewModelProvider(
            this,
            ActiveRunViewModel.Factory(repository),
        )[ActiveRunViewModel::class.java]
        executionFocused = savedInstanceState?.getBoolean("executionFocused") ?: false
        lastSpokenKey = savedInstanceState?.getString("lastSpokenKey")
        registerPage = savedInstanceState?.getInt("registerPage", 0)?.coerceAtLeast(0) ?: 0
        pendingTemplateId = savedInstanceState?.getString("pendingTemplateId")
        registerFilter = com.example.testdialer.register.RegisterFilter(
            query = savedInstanceState?.getString("registerQuery").orEmpty(),
            status = savedInstanceState?.getString("registerStatus")?.let { value -> TestRunStatus.entries.firstOrNull { it.name == value } },
            service = savedInstanceState?.getString("registerService")?.let { value -> ServiceType.entries.firstOrNull { it.name == value } },
            days = savedInstanceState?.getInt("registerDays", 0)?.takeIf { it > 0 },
        )
        pendingAdditionalType = savedInstanceState?.getString("pendingAdditionalType")?.let { saved -> TestType.entries.firstOrNull { it.name == saved } }
        dataAmountDraft = savedInstanceState?.getString("dataAmountDraft") ?: "1"
        dataUnitDraft = savedInstanceState?.getString("dataUnitDraft") ?: "MB"
        runNameDraft = savedInstanceState?.getString("runNameDraft").orEmpty()
        pendingPhoneNumber = savedInstanceState?.getString(STATE_PENDING_PHONE)
        pendingTestName = savedInstanceState?.getString(STATE_PENDING_NAME)
        dialerWasOpened = savedInstanceState?.getBoolean(STATE_DIALER_OPENED) ?: false
        awaitingVoiceOutcome = savedInstanceState?.getBoolean(STATE_AWAITING_OUTCOME) ?: false
        resultSaved = savedInstanceState?.getBoolean(STATE_RESULT_SAVED) ?: false
        voiceNameDraft = savedInstanceState?.getString(STATE_VOICE_NAME_DRAFT)
        voicePhoneDraft = savedInstanceState?.getString(STATE_VOICE_PHONE_DRAFT)
        smsLabelDraft = savedInstanceState?.getString(STATE_SMS_LABEL_DRAFT)
        smsDestinationDraft = savedInstanceState?.getString(STATE_SMS_DESTINATION_DRAFT)
        smsMessageDraft = savedInstanceState?.getString(STATE_SMS_MESSAGE_DRAFT)
        dataLabelDraft = savedInstanceState?.getString(STATE_DATA_LABEL_DRAFT)
        dataUrlDraft = savedInstanceState?.getString(STATE_DATA_URL_DRAFT)
        selectedActiveTaskId = savedInstanceState?.getString(STATE_ACTIVE_TASK_ID)?.let(::StepId)
        currentSection = savedInstanceState?.getString(STATE_CURRENT_SECTION)
            ?.let { saved -> AppSection.entries.firstOrNull { it.name == saved } }
            ?.takeUnless { it == AppSection.STATUS }
            ?: AppSection.TEST
        currentTestType = savedInstanceState?.getString(STATE_CURRENT_TEST_TYPE)
            ?.let { saved -> TestType.entries.firstOrNull { it.name == saved } } ?: TestType.VOICE

        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            setBackgroundColor(ColorPalette.background)
        }

        contentHost = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f,
            )
        }

        testSection = createTestSection()
        registerSection = createRegisterSection()

        contentHost.addView(testSection)
        contentHost.addView(registerSection)

        root.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dimen(20), dimen(4), dimen(12), dimen(4))
            addView(createStatusText("TEST DIALER").apply {
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(ColorPalette.accent)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(Button(this@MainActivity).apply {
            text = "Ustawienia"
            contentDescription = "Ustawienia wyglądu, głosu i kopii szablonów"
            setOnClickListener { showAppearanceSettings() }
            })
        })
        root.addView(contentHost)
        root.addView(createBottomNavigation())
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        setContentView(root)

        manualSessionViewModel.state.observe(this) { state ->
            manualSessionState = state
            renderManualSession()
            renderRegister()
            registerViewModel.load()
            state.message?.let { manualSessionHost.announceForAccessibility(it) }
        }
        guidedSmsViewModel.state.observe(this) { state ->
            guidedSmsState = state
            if (currentTestType == TestType.SMS) renderScenario(TestType.SMS)
            if (state.composerRequested) openSmsComposer(state)
            if (state.saved) {
                manualSessionViewModel.loadHistory()
                registerViewModel.load()
                state.completed?.let { activeRunViewModel.recordExternal(ServiceType.SMS, it) }
                testScenarioHost.announceForAccessibility(getString(R.string.sms_saved_announcement))
                announceResult("sms:${state.completed?.run?.id?.value}", "Obserwacja testu SMS została zapisana")
            }
            if (state.error != null) activeRunViewModel.cancelExecution(ServiceType.SMS)
        }
        cellularDataViewModel.state.observe(this) { state ->
            cellularDataState = state
            if (state.busy) window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            else window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            if (currentTestType == TestType.DATA) renderScenario(TestType.DATA)
            if (state.saved) {
                manualSessionViewModel.loadHistory()
                registerViewModel.load()
                state.completed?.let { activeRunViewModel.recordExternal(ServiceType.DATA, it) }
            }
            if (state.error != null) activeRunViewModel.cancelExecution(ServiceType.DATA)
        }
        activeRunViewModel.state.observe(this) { state ->
            if (activeRunState.active != null && state.active == null) executionFocused = false
            activeRunState = state
            renderActiveRun()
            if (state.active != null && !state.busy) {
                pendingTemplateId?.let { id ->
                    pendingTemplateId = null
                    templates.list().firstOrNull { it.id == id }?.let(::applyTemplate)
                }
                pendingAdditionalType?.let { type ->
                    pendingAdditionalType = null
                    selectAdditionalTest(type)
                }
            } else if (state.error != null) { pendingAdditionalType = null; pendingTemplateId = null }
            state.message?.let { runHomeView.announceForAccessibility(it) }
            if (state.active == null) selectedActiveTaskId = null
            registerViewModel.load()
        }
        registerViewModel.state.observe(this) { state ->
            registerState = state
            renderRegister()
        }
        onBackPressedDispatcher.addCallback(this) {
            if (currentSection == AppSection.REGISTER && registerState.selectedEventId != null) {
                registerViewModel.clearEvent()
            } else if (currentSection == AppSection.REGISTER && registerState.selectedRun != null) {
                registerViewModel.clearRun()
            } else if (currentSection == AppSection.TEST && executionFocused) {
                setExecutionFocus(false)
            } else if (manualSessionState.selected != null) {
                manualSessionViewModel.clearSelection()
            } else if (activeRunState.active != null || manualSessionState.active != null) {
                val sessionName = activeRunState.active?.stored?.scenario?.name
                    ?: manualSessionState.active?.stored?.scenario?.name
                    ?: getString(R.string.run_active_status)
                AlertDialog.Builder(this@MainActivity)
                    .setTitle(R.string.run_exit_active_title)
                    .setMessage(getString(R.string.run_exit_active_message, sessionName))
                    .setNegativeButton(R.string.run_exit_active_stay, null)
                    .setPositiveButton(R.string.run_exit_active_confirm) { _, _ ->
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                    .show()
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }
        reportExecutor.execute {
            val ready = (application as TestDialerApplication).annotationStore.initialize()
            runOnUiThread {
                if (!isDestroyed && !isFinishing) {
                    renderRegister()
                    if (!ready) Toast.makeText(this, "Nie udało się przygotować notatek. Stara kopia pozostaje dostępna; zapis jest wstrzymany.", Toast.LENGTH_LONG).show()
                }
            }
        }
        manualSessionViewModel.loadHistory()
        registerViewModel.load()

        showSection(currentSection)
    }

    override fun onStart() {
        super.onStart()
        registerNetworkCallback()
        refreshVoiceStatusBar()
    }

    override fun onResume() {
        super.onResume()
        refreshVoiceScenarioAfterResume()
        if (::guidedSmsViewModel.isInitialized && guidedSmsState.composerOpen) {
            guidedSmsViewModel.returnedFromComposer()
        }
    }

    private fun refreshVoiceScenarioAfterResume() {
        if (!awaitingVoiceOutcome || currentTestType != TestType.VOICE || !::testScenarioHost.isInitialized) return
        renderScenario(TestType.VOICE)
    }

    override fun onPause() {
        if (dialerWasOpened) {
            dialerWasOpened = false
            awaitingVoiceOutcome = true
        }
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("executionFocused", executionFocused)
        outState.putString("lastSpokenKey", lastSpokenKey)
        outState.putString("pendingTemplateId", pendingTemplateId)
        outState.putInt("registerPage", registerPage)
        outState.putString("registerQuery", registerFilter.query)
        outState.putString("registerStatus", registerFilter.status?.name)
        outState.putString("registerService", registerFilter.service?.name)
        outState.putInt("registerDays", registerFilter.days ?: 0)
        outState.putString("pendingAdditionalType", pendingAdditionalType?.name)
        outState.putString("dataAmountDraft", dataAmountDraft)
        outState.putString("dataUnitDraft", dataUnitDraft)
        outState.putString("runNameDraft", runNameDraft)
        outState.putString(STATE_PENDING_PHONE, pendingPhoneNumber)
        outState.putString(STATE_PENDING_NAME, pendingTestName)
        outState.putBoolean(STATE_DIALER_OPENED, dialerWasOpened)
        outState.putBoolean(STATE_AWAITING_OUTCOME, awaitingVoiceOutcome)
        outState.putBoolean(STATE_RESULT_SAVED, resultSaved)
        outState.putString(STATE_VOICE_NAME_DRAFT, voiceNameDraft)
        outState.putString(STATE_VOICE_PHONE_DRAFT, voicePhoneDraft)
        outState.putString(STATE_SMS_LABEL_DRAFT, smsLabelDraft)
        outState.putString(STATE_SMS_DESTINATION_DRAFT, smsDestinationDraft)
        outState.putString(STATE_SMS_MESSAGE_DRAFT, smsMessageDraft)
        outState.putString(STATE_DATA_LABEL_DRAFT, dataLabelDraft)
        outState.putString(STATE_DATA_URL_DRAFT, dataUrlDraft)
        outState.putString(STATE_CURRENT_SECTION, currentSection.name)
        outState.putString(STATE_CURRENT_TEST_TYPE, currentTestType.name)
        outState.putString(STATE_ACTIVE_TASK_ID, selectedActiveTaskId?.value)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        speech.close()
        reportExecutor.shutdownNow()
        super.onDestroy()
    }

    override fun onStop() {
        if (!isChangingConfigurations && cellularDataState.busy) cellularDataViewModel.cancel()
        speech.stop()
        unregisterNetworkCallback()
        super.onStop()
    }

    private fun registerNetworkCallback() {
        if (networkCallbackRegistered) return
        try {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
            networkCallbackRegistered = true
        } catch (_: SecurityException) {
            networkCallbackRegistered = false
        }
    }

    private fun unregisterNetworkCallback() {
        if (!networkCallbackRegistered) return
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (_: IllegalArgumentException) {
            // Already unregistered.
        } finally {
            networkCallbackRegistered = false
        }
    }

    private fun showSection(section: AppSection) {
        val operationalSection = if (section == AppSection.STATUS) AppSection.TEST else section
        currentSection = operationalSection
        testSection.visibility = if (operationalSection == AppSection.TEST) View.VISIBLE else View.GONE
        registerSection.visibility = if (operationalSection == AppSection.REGISTER) View.VISIBLE else View.GONE

        sectionButtons.forEach { (current, button) ->
            val selected = current == operationalSection
            button.isEnabled = true
            button.isSelected = selected
            button.alpha = if (selected) 1f else 0.86f
            button.background = pillBackground(if (selected) ColorPalette.accent else ColorPalette.button)
            button.setTextColor(if (selected) ColorPalette.onAccent else ColorPalette.textPrimary)
        }

        if (operationalSection == AppSection.TEST) {
            refreshVoiceStatusBar()
        } else if (operationalSection == AppSection.REGISTER) {
            renderRegister()
        }
    }

    private fun announceResult(key: String, message: String) {
        if (key == lastSpokenKey) return
        // Mark even a restored result as seen; only foreground completions are spoken.
        lastSpokenKey = key
        if (lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) speech.say(message)
    }

    private fun showAppearanceSettings() {
        val preferences = getSharedPreferences("ui-settings", MODE_PRIVATE)
        val enabled = preferences.getBoolean("speech", false)
        AlertDialog.Builder(this).setTitle("Ustawienia")
            .setItems(arrayOf("Motyw ekranu", if (enabled) "Wyłącz komunikaty głosowe" else "Włącz komunikaty głosowe", "Odczytaj krótkie podsumowanie", "Szablony i kopia zapasowa", "Zapisane plany sesji", "Historia i kopia zapasowa")) { _, index ->
                when (index) {
                    3 -> showTemplateBackup()
                    4 -> showScenarioPlans()
                    5 -> showHistoryBackup()
                    0 -> showThemeSettings()
                    1 -> {
                        preferences.edit().putBoolean("speech", !enabled).apply()
                        if (enabled) speech.stop() else speech.say("Komunikaty głosowe włączone")
                    }
                    2 -> {
                        if (!enabled) Toast.makeText(this, "Najpierw włącz komunikaty głosowe", Toast.LENGTH_SHORT).show()
                        else {
                            val active = activeRunState.active
                            speech.say(if (active == null) "Brak aktywnej sesji. Użyj Dodaj test, aby rozpocząć." else "Sesja ${active.stored.scenario.name}. Zapisanych zdarzeń ${active.stored.run.events.size}. Testów do wykonania ${active.tasks.count { it.status == ActiveTaskStatus.PENDING }}.")
                        }
                    }
                }
            }.setNegativeButton("Zamknij", null).show()
    }

    private fun showThemeSettings() {
        val preferences = getSharedPreferences("ui-settings", MODE_PRIVATE)
        AlertDialog.Builder(this).setTitle("Motyw ekranu")
            .setSingleChoiceItems(arrayOf("Zgodny z telefonem", "Jasny", "Ciemny"), preferences.getInt("theme", 0)) { dialog, index ->
                preferences.edit().putInt("theme", index).apply()
                dialog.dismiss()
                recreate()
            }.setNegativeButton("Zamknij", null).show()
    }

    private fun createBottomNavigation(): View {
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dimen(10), dimen(10), dimen(10), dimen(10))
            setBackgroundColor(ColorPalette.surface)
            elevation = dimen(10).toFloat()
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }

        nav.addView(createSectionButton(AppSection.TEST, getString(R.string.nav_operations)))
        nav.addView(spaceHorizontal(dimen(8)))
        nav.addView(createSectionButton(AppSection.REGISTER, getString(R.string.nav_register)))
        return nav
    }

    private fun createSectionButton(section: AppSection, label: String): Button {
        return Button(this@MainActivity).apply {
            text = label
            isAllCaps = false
            textSize = 14f
            minHeight = dimen(48)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            background = pillBackground(ColorPalette.button)
            setTextColor(ColorPalette.textPrimary)
            setOnClickListener { showSection(section) }
            sectionButtons[section] = this
        }
    }

    private fun createTestSection(): View {
        val scroll = ScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dimen(20), dimen(18), dimen(20), dimen(28))
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }

        runHomeView = RunHomeView(
            context = this,
            title = getString(R.string.run_home_title),
            description = getString(R.string.run_home_description),
            emptyTitle = getString(R.string.run_empty_title),
            emptyDescription = getString(R.string.run_empty_description),
            addTestLabel = getString(R.string.run_add_test),
            tasksTitle = getString(R.string.run_tasks_title),
            tasksDescription = getString(R.string.run_tasks_description),
            onAddTest = {
                showAddTest()
            },
        )
        systemStatusStrip = createSystemStatusStrip()
        runHomeView.statusHost.addView(systemStatusStrip)
        runHomeView.executionNavigationHost.addView(Button(this).apply {
            text = "Wróć do sesji i listy testów"
            setOnClickListener { setExecutionFocus(false) }
        })
        runHomeView.selectorHost.addView(createTestTypeSelectorCard())
        testScenarioHost = runHomeView.scenarioHost
        manualSessionHost = runHomeView.manualSessionHost
        content.addView(runHomeView)

        scroll.addView(content)
        renderScenario(currentTestType)
        renderManualSession()
        renderActiveRun()
        return scroll
    }

    private fun renderActiveRun() {
        if (!::runHomeView.isInitialized) return
        runHomeView.runHost.removeAllViews()
        runHomeView.taskListHost.removeAllViews()
        val state = activeRunState
        state.error?.let { error ->
            runHomeView.runHost.addView(createStatusText(error).apply { setTextColor(ColorPalette.bad) })
            runHomeView.runHost.addView(spaceVertical(dimen(8)))
        }
        val active = state.active
        if (active == null) {
            runHomeView.showExecutionOnly(false, false)
            runHomeView.executionContextHost.removeAllViews()
            runHomeView.runHost.addView(createCard {
                addView(createCardTitle(getString(R.string.run_empty_title)))
                addView(spaceVertical(dimen(6)))
                addView(createBodyText(getString(R.string.run_start_description)))
                addView(spaceVertical(dimen(12)))
                val name = createOptionalInput(getString(R.string.run_name_hint)).apply {
                    setText(runNameDraft)
                    trackDraft { runNameDraft = it }
                }
                addView(name)
                addView(spaceVertical(dimen(10)))
                addView(Button(this@MainActivity).apply {
                    setText(R.string.run_start_empty)
                    isAllCaps = false
                    minHeight = dimen(50)
                    isEnabled = !state.busy && !state.executionInProgress
                    background = pillBackground(ColorPalette.accent)
                    setTextColor(ColorPalette.onAccent)
                    setOnClickListener { activeRunViewModel.startEmpty(name.text.toString()) }
                })
                addView(spaceVertical(dimen(8)))
                addView(Button(this@MainActivity).apply {
                    setText(R.string.run_start_scenario)
                    isAllCaps = false
                    minHeight = dimen(50)
                    isEnabled = !state.busy && !state.executionInProgress
                    setOnClickListener { activeRunViewModel.startScenario(LocalScenarioCatalog.smoke) }
                })
                addView(spaceVertical(dimen(8)))
                addView(reportButton(getString(R.string.quota_scenario)) { startDataScenario() }.apply {
                    isEnabled = !state.busy && !state.executionInProgress
                })
            })
            runHomeView.selectorHost.visibility = View.GONE
            runHomeView.scenarioHost.visibility = View.GONE
            runHomeView.manualSessionHost.visibility = View.GONE
            return
        }

        val run = active.stored.run
        runHomeView.runHost.addView(createCard {
            addView(createCardTitle(active.stored.scenario.name).apply { ViewCompat.setAccessibilityHeading(this, true) })
            addView(spaceVertical(dimen(6)))
            addView(createTag(getString(R.string.run_active_status)))
            addView(spaceVertical(dimen(10)))
            val done = active.tasks.count { it.status == ActiveTaskStatus.DONE }
            val skipped = active.tasks.count { it.status == ActiveTaskStatus.SKIPPED }
            addView(createBodyText(getString(R.string.run_progress_summary, done, active.tasks.size, skipped, run.events.size)))
            if (active.tasks.isNotEmpty()) {
                addView(spaceVertical(dimen(8)))
                addView(ProgressBar(this@MainActivity, null, android.R.attr.progressBarStyleHorizontal).apply {
                    max = active.tasks.size
                    progress = done + skipped
                    progressTintList = ColorStateList.valueOf(ColorPalette.accent)
                    contentDescription = getString(R.string.run_progress_accessibility, done + skipped, active.tasks.size)
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dimen(8))
                })
            }
            addView(spaceVertical(dimen(6)))
            addView(createMicroText(getString(R.string.manual_session_run_id, run.id.value)).apply { setTextIsSelectable(true) })
            addView(spaceVertical(dimen(8)))
            addView(createBodyText(getString(R.string.data_run_total, dataBytes(run))))
            addView(spaceVertical(dimen(8)))
            addView(Button(this@MainActivity).apply {
                setText(R.string.run_note)
                setOnClickListener { editRunNote(run.id) }
            })
            addView(spaceVertical(dimen(10)))
            addView(Button(this@MainActivity).apply {
                setText(R.string.run_complete)
                isAllCaps = false
                minHeight = dimen(48)
                isEnabled = !state.busy && !state.executionInProgress
                background = pillBackground(ColorPalette.ok)
                setTextColor(ColorPalette.onAccent)
                setOnClickListener { requestRunCompletion() }
            })
        })
        if (active.tasks.isEmpty()) {
            runHomeView.taskListHost.addView(createBodyText(getString(R.string.run_no_planned_tasks)))
        } else active.tasks.forEach { task ->
            runHomeView.taskListHost.addView(createCard {
                addView(createCardTitle(task.step.title))
                addView(spaceVertical(dimen(4)))
                addView(createStatusText(when (task.status) {
                    ActiveTaskStatus.PENDING -> getString(R.string.task_pending)
                    ActiveTaskStatus.DONE -> getString(R.string.task_done)
                    ActiveTaskStatus.SKIPPED -> getString(R.string.task_skipped)
                }))
                if (task.status == ActiveTaskStatus.PENDING) {
                    addView(spaceVertical(dimen(8)))
                    addView(Button(this@MainActivity).apply {
                        setText(R.string.task_open)
                        contentDescription = getString(R.string.task_open_accessibility, task.step.title)
                        isAllCaps = false
                        minHeight = dimen(48)
                        isEnabled = !state.busy && !state.executionInProgress
                        setOnClickListener { openActiveTask(task.step.id, task.step.action) }
                    })
                    addView(spaceVertical(dimen(6)))
                    addView(Button(this@MainActivity).apply {
                        setText(R.string.task_skip)
                        contentDescription = getString(R.string.task_skip_accessibility, task.step.title)
                        isAllCaps = false
                        minHeight = dimen(48)
                        isEnabled = !state.busy && !state.executionInProgress
                        setOnClickListener { requestTaskSkip(task.step.id) }
                    })
                }
            })
            runHomeView.taskListHost.addView(spaceVertical(dimen(8)))
        }
        runHomeView.selectorHost.visibility = View.VISIBLE
        runHomeView.scenarioHost.visibility = View.VISIBLE
        runHomeView.manualSessionHost.visibility = View.GONE
        renderExecutionContext(currentTestType)
        runHomeView.showExecutionOnly(executionFocused)
    }

    private fun requestRunCompletion() {
        val state = activeRunViewModel.state.value ?: return
        val active = state.active ?: return
        if (state.busy || state.executionInProgress || activeRunViewModel.executionInProgress()) return
        val pending = active.tasks.filter { it.status == ActiveTaskStatus.PENDING }
        if (pending.isEmpty()) {
            activeRunViewModel.complete()
            return
        }
        val runId = active.stored.run.id
        AlertDialog.Builder(this)
            .setTitle(R.string.run_complete_pending_title)
            .setMessage(getString(
                R.string.run_complete_pending_message,
                pending.size,
                pending.joinToString("\n") { "• ${it.step.title}" },
            ))
            .setNegativeButton(R.string.run_complete_keep_testing, null)
            .setPositiveButton(R.string.run_complete_confirm) { _, _ ->
                val current = activeRunViewModel.state.value
                if (current?.active?.stored?.run?.id == runId && !current.busy &&
                    !current.executionInProgress && !activeRunViewModel.executionInProgress()
                ) activeRunViewModel.complete()
            }
            .show()
    }

    private fun requestTaskSkip(stepId: StepId) {
        val state = activeRunViewModel.state.value ?: return
        val active = state.active ?: return
        if (state.busy || state.executionInProgress || activeRunViewModel.executionInProgress()) return
        val task = active.tasks.singleOrNull {
            it.step.id == stepId && it.status == ActiveTaskStatus.PENDING
        } ?: return
        val runId = active.stored.run.id
        AlertDialog.Builder(this)
            .setTitle(R.string.task_skip_confirm_title)
            .setMessage(getString(R.string.task_skip_confirm_message, task.step.title))
            .setNegativeButton(R.string.task_skip_cancel, null)
            .setPositiveButton(R.string.task_skip_confirm) { _, _ ->
                val current = activeRunViewModel.state.value
                val stillPending = current?.active?.tasks?.any {
                    it.step.id == stepId && it.status == ActiveTaskStatus.PENDING
                } == true
                if (current?.active?.stored?.run?.id == runId && stillPending && !current.busy &&
                    !current.executionInProgress && !activeRunViewModel.executionInProgress()
                ) activeRunViewModel.skip(stepId)
            }
            .show()
    }

    private fun openActiveTask(stepId: StepId?, action: TestAction) {
        if (activeRunViewModel.executionInProgress() || isTestTypeSwitchLocked()) {
            Toast.makeText(this, R.string.test_already_in_progress, Toast.LENGTH_LONG).show()
            return
        }
        when (action) {
            is TestAction.Voice -> {
                awaitingVoiceOutcome = false
                resultSaved = false
                pendingPhoneNumber = null
                pendingTestName = null
                voiceNameDraft = null
                voicePhoneDraft = action.destination
            }
            is TestAction.Sms -> {
                guidedSmsViewModel.startAnother()
                smsLabelDraft = null
                smsDestinationDraft = action.destination
                smsMessageDraft = action.message.orEmpty()
            }
            is TestAction.Data -> {
                cellularDataViewModel.startAnother()
                dataLabelDraft = null
                dataUrlDraft = action.target
                activeRunState.active?.tasks?.firstOrNull { it.step.id == stepId }?.let { task ->
                    if (task.requestedBytes != null) { dataAmountDraft = task.requestedBytes.toString(); dataUnitDraft = "B" }
                    else if (task.requireDataAmountSelection) { dataAmountDraft = ""; dataUnitDraft = "MB" }
                }
            }
        }
        selectedActiveTaskId = stepId
        currentTestType = when (action) {
            is TestAction.Voice -> TestType.VOICE
            is TestAction.Sms -> TestType.SMS
            is TestAction.Data -> TestType.DATA
        }
        updateTestTypeChips()
        renderScenario(currentTestType)
        setExecutionFocus(true)
        testScenarioHost.isFocusableInTouchMode = true
        testScenarioHost.requestFocus()
        testScenarioHost.announceForAccessibility(getString(R.string.task_opened_announcement, currentTestType.name))
        testScenarioHost.post {
            testScenarioHost.requestRectangleOnScreen(android.graphics.Rect(0, 0, testScenarioHost.width, dimen(100)), false)
        }
    }

    private fun setExecutionFocus(focused: Boolean) {
        executionFocused = focused
        runHomeView.showExecutionOnly(focused && activeRunState.active != null, activeRunState.active != null)
        if (!focused) {
            (getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager)
                .hideSoftInputFromWindow(testScenarioHost.windowToken, 0)
            runHomeView.runHost.isFocusableInTouchMode = true
            runHomeView.runHost.requestFocus()
        }
    }

    private fun showAddTest() {
        if (activeRunState.busy || activeRunViewModel.executionInProgress() || isTestTypeSwitchLocked()) {
            Toast.makeText(this, R.string.test_already_in_progress, Toast.LENGTH_LONG).show()
            return
        }
        AlertDialog.Builder(this).setTitle("Dodaj test")
            .setItems(arrayOf("Połączenie", "SMS", "Dane", "Zapisane szablony")) { _, index ->
                if (index == 3) { showTemplates(); return@setItems }
                val type = TestType.entries[index]
                if (activeRunState.active == null) {
                    pendingAdditionalType = type
                    activeRunViewModel.startEmpty(runNameDraft.ifBlank { "Szybki test" })
                } else selectAdditionalTest(type)
            }.setNegativeButton("Anuluj", null).show()
    }

    private fun selectAdditionalTest(type: TestType) {
        if (isTestTypeSwitchLocked() || activeRunViewModel.executionInProgress()) return
        selectedActiveTaskId = null
        clearDraft(type)
        if (type == TestType.VOICE) resultSaved = false
        if (type == TestType.SMS) guidedSmsViewModel.startAnother()
        if (type == TestType.DATA) cellularDataViewModel.startAnother()
        currentTestType = type
        updateTestTypeChips()
        renderScenario(type)
        showSection(AppSection.TEST)
        setExecutionFocus(true)
        testScenarioHost.isFocusableInTouchMode = true
        testScenarioHost.requestFocus()
        testScenarioHost.post {
            testScenarioHost.requestRectangleOnScreen(android.graphics.Rect(0, 0, testScenarioHost.width, dimen(100)), false)
        }
    }

    private fun selectedTaskAction(): TestAction? = activeRunState.active?.tasks
        ?.singleOrNull { it.step.id == selectedActiveTaskId }
        ?.step?.action

    private fun createRegisterSection(): View {
        val scroll = ScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dimen(20), dimen(18), dimen(20), dimen(28))
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }

        content.addView(createSectionHeader(
            getString(R.string.register_title),
            getString(R.string.register_description),
        ))
        content.addView(spaceVertical(dimen(16)))
        registerFiltersHost = createRegisterFilters()
        content.addView(registerFiltersHost)
        content.addView(spaceVertical(dimen(12)))
        registerListHost = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        content.addView(registerListHost)
        renderRegister()

        scroll.addView(content)
        return scroll
    }

    private fun renderManualSession() {
        if (!::manualSessionHost.isInitialized) return
        manualSessionHost.removeAllViews()
        val state = manualSessionState
        manualSessionHost.addView(createCard {
            addView(createCardTitle(getString(R.string.manual_session_title)).apply {
                ViewCompat.setAccessibilityHeading(this, true)
            })
            addView(spaceVertical(dimen(8)))
            addView(createBodyText(getString(R.string.manual_session_description)))
            addView(spaceVertical(dimen(14)))

            state.error?.let {
                addView(createStatusText(it).apply {
                    setTextColor(ColorPalette.bad)
                    accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_ASSERTIVE
                })
                addView(spaceVertical(dimen(12)))
            }

            val active = state.active
            if (active == null) {
                val nameInput = createOptionalInput(getString(R.string.manual_session_name_hint))
                val targetInput = createOptionalInput(getString(R.string.manual_session_target_hint))
                addView(nameInput)
                addView(spaceVertical(dimen(10)))
                addView(targetInput)
                addView(spaceVertical(dimen(12)))
                addView(Button(this@MainActivity).apply {
                    setText(R.string.manual_session_start)
                    isAllCaps = false
                    textSize = 17f
                    minHeight = dimen(52)
                    isEnabled = !state.busy
                    background = pillBackground(ColorPalette.accent)
                    setTextColor(ColorPalette.onAccent)
                    setOnClickListener {
                        val name = nameInput.text.toString().trim()
                        val target = targetInput.text.toString().trim()
                        if (name.isBlank() || target.isBlank()) {
                            Toast.makeText(
                                this@MainActivity,
                                R.string.manual_session_required,
                                Toast.LENGTH_LONG,
                            ).show()
                        } else {
                            manualSessionViewModel.start(name, currentTestType.toServiceType(), target)
                        }
                    }
                })
            } else {
                val run = active.stored.run
                addView(createBodyText(getString(R.string.manual_session_active, active.stored.scenario.name)))
                addView(spaceVertical(dimen(6)))
                addView(createMicroText(getString(R.string.manual_session_run_id, run.id.value)).apply {
                    setTextIsSelectable(true)
                })
                addView(spaceVertical(dimen(12)))
                if (!active.eventRecorded) {
                    addView(Button(this@MainActivity).apply {
                        setText(R.string.manual_session_record)
                        isAllCaps = false
                        textSize = 17f
                        minHeight = dimen(52)
                        isEnabled = !state.busy
                        background = pillBackground(ColorPalette.accent)
                        setTextColor(ColorPalette.onAccent)
                        setOnClickListener { manualSessionViewModel.recordEvent() }
                    })
                } else {
                    addView(createStatusText(getString(R.string.manual_session_recorded)).apply {
                        accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
                    })
                    addView(spaceVertical(dimen(12)))
                    addView(Button(this@MainActivity).apply {
                        setText(R.string.manual_session_complete)
                        isAllCaps = false
                        textSize = 17f
                        minHeight = dimen(52)
                        isEnabled = !state.busy
                        background = pillBackground(ColorPalette.ok)
                        setTextColor(ColorPalette.onAccent)
                        setOnClickListener { manualSessionViewModel.complete() }
                    })
                }
            }
            if (state.busy) {
                addView(spaceVertical(dimen(10)))
                addView(createStatusText(getString(R.string.manual_session_saving)).apply {
                    accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
                })
            }
        })
    }

    private fun TestType.toServiceType(): ServiceType = when (this) {
        TestType.VOICE -> ServiceType.VOICE
        TestType.SMS -> ServiceType.SMS
        TestType.DATA -> ServiceType.DATA
    }

    private fun renderRegister() {
        if (!::registerListHost.isInitialized) return
        if (::registerFilterLabel.isInitialized) {
            val filters = listOfNotNull(registerFilter.status?.let(::localizeRunStatus),
                registerFilter.service?.let { when (it) { ServiceType.VOICE -> "Połączenie"; ServiceType.SMS -> "SMS"; ServiceType.DATA -> "Dane" } },
                registerFilter.days?.let { if (it == 1) "Dzisiaj" else "$it dni" })
            registerFilterLabel.text = if (filters.isEmpty()) "Filtry: wszystkie sesje" else "Filtry: ${filters.joinToString(" · ")}"
        }
        registerFiltersHost.visibility = if (registerState.selectedRun == null) View.VISIBLE else View.GONE
        registerListHost.removeAllViews()
        registerState.selectedRun?.let { selected ->
            if (registerState.selectedEventId != null) {
                selected.run.events.singleOrNull { it.id == registerState.selectedEventId }
                    ?.let { event -> registerListHost.addView(createEventDetail(selected, event)) }
                    ?: registerListHost.addView(createRegisterError(getString(R.string.register_event_missing)))
            } else {
                registerListHost.addView(createRunDetail(selected))
            }
            registerState.error?.let { error ->
                registerListHost.addView(spaceVertical(dimen(12)))
                registerListHost.addView(createRegisterError(error))
            }
            return
        }

        registerListHost.addView(createCard {
            addView(createCardTitle(getString(R.string.manual_history_title)).apply {
                ViewCompat.setAccessibilityHeading(this, true)
            })
            addView(spaceVertical(dimen(8)))
            addView(createBodyText(getString(R.string.register_runs_description)))
        })
        registerState.error?.let { error ->
            registerListHost.addView(spaceVertical(dimen(12)))
            registerListHost.addView(createRegisterError(error))
        }
        if (registerState.busy && registerState.runs.isEmpty()) {
            registerListHost.addView(spaceVertical(dimen(12)))
            registerListHost.addView(createBodyText(getString(R.string.register_loading)))
        }
        val summaries = registerFilter.apply(registerState.runs, System.currentTimeMillis())
        val pageCount = ((summaries.size + registerPageSize - 1) / registerPageSize).coerceAtLeast(1)
        registerPage = registerPage.coerceIn(0, pageCount - 1)
        registerListHost.addView(createStatusText("Znaleziono sesji: ${summaries.size} / ${registerState.runs.size}"))
        if (pageCount > 1) registerListHost.addView(createRegisterPager(pageCount))
        if (summaries.isEmpty()) {
            registerListHost.addView(spaceVertical(dimen(12)))
            registerListHost.addView(createCard {
                addView(createCardTitle(getString(R.string.register_runs_empty_title)))
                addView(spaceVertical(dimen(8)))
                addView(createBodyText(if (registerState.runs.isEmpty()) getString(R.string.register_runs_empty_body) else "Brak sesji pasujących do filtrów. Zmień wyszukiwanie lub wyczyść filtry."))
            })
        } else {
            summaries.drop(registerPage * registerPageSize).take(registerPageSize).forEach { summary ->
                registerListHost.addView(spaceVertical(dimen(12)))
                registerListHost.addView(createRunSummaryCard(summary))
            }
        }
        if (pageCount > 1) registerListHost.addView(createRegisterPager(pageCount))
        registerListHost.addView(spaceVertical(dimen(20)))
        registerListHost.addView(createCard {
            elevation = 0f
            setPadding(dimen(14), dimen(14), dimen(14), dimen(14))
            addView(createStatusText(getString(R.string.legacy_voice_history_title)).apply {
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                ViewCompat.setAccessibilityHeading(this, true)
            })
            addView(spaceVertical(dimen(6)))
            addView(createStatusText(getString(R.string.legacy_voice_history_description)))
        })
        registerListHost.addView(spaceVertical(dimen(12)))
        val results = voiceResultStore.loadAll()
        if (results.isEmpty()) {
            registerListHost.addView(createCard {
                addView(createCardTitle(getString(R.string.register_empty_title)))
                addView(spaceVertical(dimen(8)))
                addView(createBodyText(getString(R.string.register_empty_body)))
            })
            return
        }
        results.forEachIndexed { index, result ->
            if (index > 0) registerListHost.addView(spaceVertical(dimen(12)))
            registerListHost.addView(createVoiceResultCard(result))
        }
    }

    private fun createRegisterPager(pageCount: Int): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        val label = createStatusText("Strona ${registerPage + 1} z $pageCount · do $registerPageSize sesji").apply {
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        addView(label)
        fun navigate(delta: Int) {
            registerPage += delta
            renderRegister()
            registerFiltersHost.requestRectangleOnScreen(android.graphics.Rect(0, 0, registerFiltersHost.width, registerFiltersHost.height), false)
            registerListHost.announceForAccessibility("Strona ${registerPage + 1} z $pageCount")
        }
        // Vertical controls preserve large-font labels on narrow phones.
        addView(Button(this@MainActivity).apply {
            text = "Poprzednia strona"; isAllCaps = false; minHeight = dimen(48)
            isEnabled = registerPage > 0
            setOnClickListener { navigate(-1) }
        })
        addView(Button(this@MainActivity).apply {
            text = "Następna strona"; isAllCaps = false; minHeight = dimen(48)
            isEnabled = registerPage + 1 < pageCount
            setOnClickListener { navigate(1) }
        })
    }

    private fun createRegisterFilters(): LinearLayout = createCard {
        val search = createOptionalInput("Szukaj nazwy sesji lub ID").apply {
            setText(registerFilter.query)
            contentDescription = "Wyszukiwanie sesji po nazwie lub identyfikatorze"
            isSingleLine = true
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
            setOnEditorActionListener { view, action, _ ->
                if (action == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                    (getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager).hideSoftInputFromWindow(view.windowToken, 0)
                    clearFocus(); true
                } else false
            }
            trackDraft { query -> registerPage = 0; registerFilter = registerFilter.copy(query = query); renderRegister() }
        }
        addView(search)
        addView(Button(this@MainActivity).apply {
            registerFilterLabel = this
            text = "Filtry: wszystkie sesje"
            setOnClickListener { showRegisterFilters() }
        })
        addView(Button(this@MainActivity).apply {
            text = "Wyczyść filtry"
            setOnClickListener { registerPage = 0; registerFilter = com.example.testdialer.register.RegisterFilter(); search.setText(""); renderRegister() }
        })
    }

    private fun showRegisterFilters() {
        AlertDialog.Builder(this).setTitle("Filtr rejestru")
            .setItems(arrayOf("Status sesji", "Typ usługi", "Data rozpoczęcia")) { _, category ->
                val options = when (category) {
                    0 -> arrayOf("Wszystkie", "Utworzona", "W toku", "Zakończona", "Przerwana")
                    1 -> arrayOf("Wszystkie", "Połączenie", "SMS", "Dane")
                    else -> arrayOf("Cała historia", "Dzisiaj", "Ostatnie 7 dni", "Ostatnie 30 dni")
                }
                AlertDialog.Builder(this).setTitle("Wybierz filtr").setItems(options) { _, choice ->
                    registerPage = 0
                    registerFilter = when (category) {
                        0 -> registerFilter.copy(status = if (choice == 0) null else TestRunStatus.entries[choice - 1])
                        1 -> registerFilter.copy(service = if (choice == 0) null else ServiceType.entries[choice - 1])
                        else -> registerFilter.copy(days = listOf(null, 1, 7, 30)[choice])
                    }
                    renderRegister()
                }.setNegativeButton("Anuluj", null).show()
            }.setNegativeButton("Zamknij", null).show()
    }

    private fun createRegisterError(message: String): View = createCard {
        isFocusable = true
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        contentDescription = message
        addView(createStatusText(message).apply {
            setTextColor(ColorPalette.bad)
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_ASSERTIVE
        })
    }

    private fun createRunSummaryCard(summary: TestRunSummary): View {
        val status = localizeRunStatus(summary.status)
        val date = formatDate(summary.startedAtMillis)
        return createCard {
            isClickable = true
            isFocusable = true
            minimumHeight = dimen(96)
            contentDescription = getString(
                R.string.register_run_item_accessibility,
                summary.scenarioName,
                status,
                date,
                summary.eventCount,
            )
            addView(createCardTitle(summary.scenarioName))
            if (billingReviews.interruptedAt(summary.runId) > 0) addView(createTag("Przegląd: sesja przerwana"))
            addView(spaceVertical(dimen(6)))
            addView(createBodyText(getString(R.string.register_run_item_status, status)))
            addView(spaceVertical(dimen(4)))
            addView(createBodyText(getString(R.string.register_run_item_started, date)))
            addView(spaceVertical(dimen(4)))
            addView(createBodyText(getString(R.string.register_run_item_events, summary.eventCount)))
            addView(spaceVertical(dimen(8)))
            addView(Button(this@MainActivity).apply {
                setText(R.string.register_open_run)
                isAllCaps = false
                minHeight = dimen(48)
                contentDescription = getString(R.string.register_open_run_accessibility, summary.scenarioName)
                setOnClickListener { registerViewModel.selectRun(summary.runId) }
            })
            setOnClickListener { registerViewModel.selectRun(summary.runId) }
        }
    }

    private fun canMarkInterrupted(id: RunId, status: TestRunStatus): Boolean =
        status == TestRunStatus.RUNNING && activeRunState.active?.stored?.run?.id != id &&
            manualSessionState.active?.stored?.run?.id != id && !isTestTypeSwitchLocked() && !activeRunViewModel.executionInProgress()

    private fun createRunDetail(stored: StoredTestRun): View {
        val run = stored.run
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(Button(this@MainActivity).apply {
                setText(R.string.register_back_to_runs)
                isAllCaps = false
                minHeight = dimen(48)
                contentDescription = getString(R.string.register_back_to_runs_accessibility)
                setOnClickListener { registerViewModel.clearRun() }
            })
            addView(spaceVertical(dimen(12)))
            addView(createCard {
                addView(createHeaderText(stored.scenario.name).apply {
                    ViewCompat.setAccessibilityHeading(this, true)
                })
                addView(spaceVertical(dimen(8)))
                addView(createBodyText(getString(R.string.register_run_detail_status, localizeRunStatus(run.status))))
                addView(spaceVertical(dimen(5)))
                addView(createBodyText(getString(R.string.register_run_detail_started, formatDateWithMillis(run.startedAtMillis))))
                run.completedAtMillis?.let {
                    addView(spaceVertical(dimen(5)))
                    addView(createBodyText(getString(R.string.register_run_detail_completed, formatDateWithMillis(it))))
                }
                addView(spaceVertical(dimen(8)))
                addView(optionalFields("Identyfikatory techniczne") {
                addView(createMicroText(getString(R.string.register_run_detail_id, run.id.value)).apply {
                    setTextIsSelectable(true)
                })
                addView(spaceVertical(dimen(4)))
                addView(createMicroText(getString(R.string.register_run_detail_scenario, run.scenarioId.value, run.scenarioVersion)))
                })
                addView(spaceVertical(dimen(14)))
                if (canMarkInterrupted(run.id, run.status)) {
                    val interrupted = billingReviews.interruptedAt(run.id)
                    addView(createBodyText(if (interrupted > 0) "Oznaczona jako przerwana przez testera: ${formatDateWithMillis(interrupted)}. Historyczny stan zapisu pozostaje w toku." else "Ta sesja pozostała w toku po przerwaniu pracy. Pobieranie nie jest wznawiane. Możesz zachować historię i rozpocząć nową sesję."))
                    if (interrupted == 0L) addView(Button(this@MainActivity).apply {
                        text = "Oznacz przegląd sesji jako przerwany"
                        setOnClickListener {
                            AlertDialog.Builder(this@MainActivity).setTitle("Oznaczyć sesję jako przerwaną?")
                                .setMessage("Zapiszę oznaczenie testera. Zdarzenia, czasy i wyniki sesji pozostaną zachowane.")
                                .setPositiveButton("Oznacz") { _, _ ->
                                    reportExecutor.execute {
                                        val saved = billingReviews.markInterrupted(run.id, System.currentTimeMillis())
                                        runOnUiThread { if (!isDestroyed && !isFinishing) { if (saved) renderRegister() else Toast.makeText(this@MainActivity, "Nie udało się zapisać oznaczenia", Toast.LENGTH_LONG).show() } }
                                    }
                                }.setNegativeButton("Anuluj", null).show()
                        }
                    })
                }
                addView(reportButton(getString(R.string.report_export)) { showReportOptions(stored) })
                addView(reportButton("Porównaj z inną sesją") { chooseComparison(stored.run.id) })
                addView(reportButton("Przygotuj powtórkę sesji") { prepareSessionRepeat(stored.run.id) })
                addView(spaceVertical(dimen(6)))
                addView(createStatusText(getString(R.string.report_contents)))
                addView(spaceVertical(dimen(10)))
                addView(createBodyText(getString(R.string.data_run_total, dataBytes(run))))
                addView(spaceVertical(dimen(8)))
                addView(Button(this@MainActivity).apply {
                    setText(R.string.run_note)
                    setOnClickListener { editRunNote(run.id) }
                })
                val note = runNotes.get(run.id)
                if (note.isNotBlank()) addView(createBodyText(note).apply { setTextIsSelectable(true) })
            })
            addView(spaceVertical(dimen(12)))
            addView(createCard {
                addView(createCardTitle(getString(R.string.register_events_title)).apply {
                    ViewCompat.setAccessibilityHeading(this, true)
                })
                addView(spaceVertical(dimen(6)))
                addView(createBodyText(getString(R.string.register_events_description)))
            })
            if (run.events.isEmpty()) {
                addView(spaceVertical(dimen(12)))
                addView(createCard { addView(createBodyText(getString(R.string.register_events_empty))) })
            } else {
                run.events.forEachIndexed { index, event ->
                    addView(spaceVertical(dimen(12)))
                    addView(createEventSummaryCard(event, index))
                }
            }
        }
    }

    private fun createEventSummaryCard(event: TestEvent, index: Int): View {
        val type = eventTypeLabel(event)
        val result = event.observation?.let { observationLabel(it.status) }
            ?: getString(R.string.register_observation_none)
        return createCard {
            isClickable = true
            isFocusable = true
            contentDescription = getString(
                R.string.register_event_item_accessibility,
                index + 1,
                type,
                formatDateWithMillis(event.occurredAtMillis),
                result,
            )
            addView(createCardTitle(getString(R.string.register_event_number, index + 1)))
            addView(spaceVertical(dimen(5)))
            addView(createBodyText(getString(R.string.register_event_item_type, type)))
            addView(spaceVertical(dimen(4)))
            addView(createBodyText(getString(R.string.register_event_item_time, formatDateWithMillis(event.occurredAtMillis))))
            addView(spaceVertical(dimen(4)))
            addView(createBodyText(getString(R.string.register_event_item_result, result)))
            addView(spaceVertical(dimen(8)))
            addView(Button(this@MainActivity).apply {
                setText(R.string.register_open_event)
                isAllCaps = false
                minHeight = dimen(48)
                contentDescription = getString(R.string.register_open_event_accessibility, type)
                setOnClickListener { registerViewModel.selectEvent(event.id) }
            })
            setOnClickListener { registerViewModel.selectEvent(event.id) }
        }
    }

    private fun createEventDetail(stored: StoredTestRun, event: TestEvent): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(Button(this@MainActivity).apply {
                setText(R.string.register_back_to_run)
                isAllCaps = false
                minHeight = dimen(48)
                contentDescription = getString(R.string.register_back_to_run_accessibility)
                setOnClickListener { registerViewModel.clearEvent() }
            })
            addView(spaceVertical(dimen(12)))
            addView(createCard {
                addView(createHeaderText(getString(R.string.register_event_detail_title)).apply {
                    ViewCompat.setAccessibilityHeading(this, true)
                })
                addView(spaceVertical(dimen(8)))
                addView(createBodyText(getString(R.string.register_event_detail_type, eventTypeLabel(event))))
                addView(spaceVertical(dimen(5)))
                addView(createBodyText(getString(R.string.register_event_detail_time, formatDateWithMillis(event.occurredAtMillis))))
                addView(spaceVertical(dimen(10)))
                addView(optionalFields("Identyfikatory techniczne") {
                addView(createMicroText(getString(R.string.register_event_detail_event_id, event.id.value)).apply { setTextIsSelectable(true) })
                addView(spaceVertical(dimen(4)))
                addView(createMicroText(getString(R.string.register_event_detail_run_id, event.runId.value)).apply { setTextIsSelectable(true) })
                addView(spaceVertical(dimen(4)))
                addView(createMicroText(getString(R.string.register_event_detail_step_id, event.stepId.value)).apply { setTextIsSelectable(true) })
                })
                addView(spaceVertical(dimen(12)))
                addView(reportButton(getString(R.string.repeat_event)) {
                    if (activeRunState.active == null) {
                        Toast.makeText(this@MainActivity, R.string.repeat_needs_run, Toast.LENGTH_LONG).show()
                    } else if (!activeRunState.busy && !isTestTypeSwitchLocked() && !activeRunViewModel.executionInProgress()) {
                        if (event.action is TestAction.Data) {
                            event.correlation.references.firstOrNull { it.namespace == "requestedBytes" }?.value
                                ?.toLongOrNull()?.takeIf { it in 1..com.example.testdialer.data.DataVolume.MAX_BYTES }?.let {
                                    dataAmountDraft = it.toString()
                                    dataUnitDraft = "B"
                                }
                        }
                        openActiveTask(null, event.action)
                        showSection(AppSection.TEST)
                        testScenarioHost.requestFocus()
                        testScenarioHost.post { testScenarioHost.requestRectangleOnScreen(android.graphics.Rect(0, 0, testScenarioHost.width, dimen(100)), false) }
                    } else {
                        Toast.makeText(this@MainActivity, R.string.test_already_in_progress, Toast.LENGTH_LONG).show()
                    }
                })
                addView(spaceVertical(dimen(6)))
                addView(createStatusText(getString(R.string.repeat_event_description)))
                addView(spaceVertical(dimen(8)))
                addView(Button(this@MainActivity).apply {
                    text = "Zapisz jako szablon"
                    setOnClickListener { saveTemplate(event) }
                })
            })
            addView(spaceVertical(dimen(12)))
            addView(createCard {
                addView(createCardTitle(getString(R.string.register_parameters_title)).apply { ViewCompat.setAccessibilityHeading(this, true) })
                addView(spaceVertical(dimen(8)))
                eventParameters(event).forEachIndexed { index, parameter ->
                    if (index > 0) addView(spaceVertical(dimen(6)))
                    addView(createBodyText(parameter))
                }
            })
            addView(spaceVertical(dimen(12)))
            addView(createCard {
                addView(createCardTitle("Rozliczenie — ocena testera"))
                val review = billingReviews.get(event.id)
                addView(createBodyText("Oczekiwano: ${review.expected.ifBlank { "nie określono" }}"))
                addView(createBodyText("Otrzymano: ${review.actual.ifBlank { "nie sprawdzono" }}"))
                addView(createTag("Ocena rozliczenia: ${billingVerdictLabel(review.verdict)}"))
                if (review.reviewedAtMillis > 0) addView(createStatusText("Ocena testera z ${formatDateWithMillis(review.reviewedAtMillis)}"))
                stored.scenario.steps.firstOrNull { it.id == event.stepId }?.expectedResult?.let {
                    addView(createStatusText("Oczekiwanie scenariusza: ${it.description}"))
                }
                addView(Button(this@MainActivity).apply {
                    text = "Edytuj oczekiwanie i ocenę"
                    setOnClickListener { editBillingReview(event.id) }
                })
                addView(spaceVertical(dimen(12)))
                addView(createCardTitle(getString(R.string.register_observation_title)).apply { ViewCompat.setAccessibilityHeading(this, true) })
                addView(spaceVertical(dimen(8)))
                val observation = event.observation
                if (observation == null) {
                    addView(createBodyText(getString(R.string.register_observation_none)))
                } else {
                    addView(createBodyText(getString(R.string.register_observation_status, observationLabel(observation.status))))
                    addView(spaceVertical(dimen(5)))
                    addView(createBodyText(getString(R.string.register_observation_source, observation.source.name)))
                    addView(spaceVertical(dimen(5)))
                    addView(createBodyText(getString(R.string.register_observation_code, observation.code)))
                    observation.description?.let {
                        addView(spaceVertical(dimen(5)))
                        addView(createBodyText(getString(R.string.register_observation_description, it)))
                    }
                }
            })
            addView(spaceVertical(dimen(12)))
            addView(createCard {
                addView(createCardTitle(getString(R.string.register_correlation_title)).apply { ViewCompat.setAccessibilityHeading(this, true) })
                addView(spaceVertical(dimen(8)))
                val correlation = event.correlation
                addView(createBodyText(getString(R.string.register_correlation_source, correlation.sourceAddress ?: getString(R.string.register_value_missing))))
                addView(spaceVertical(dimen(5)))
                addView(createBodyText(getString(R.string.register_correlation_destination, correlation.destinationAddress ?: getString(R.string.register_value_missing))))
                addView(spaceVertical(dimen(5)))
                addView(createBodyText(getString(R.string.register_correlation_subscriber, correlation.subscriberAlias ?: getString(R.string.register_value_missing))))
                if (correlation.references.isEmpty()) {
                    addView(spaceVertical(dimen(5)))
                    addView(createBodyText(getString(R.string.register_correlation_references_none)))
                } else correlation.references.forEach { reference ->
                    addView(spaceVertical(dimen(5)))
                    addView(createBodyText(getString(R.string.register_correlation_reference, reference.namespace, reference.value)))
                }
                addView(spaceVertical(dimen(10)))
                addView(Button(this@MainActivity).apply {
                    setText(R.string.register_copy_correlation)
                    isAllCaps = false
                    minHeight = dimen(48)
                    contentDescription = getString(R.string.register_copy_correlation_accessibility)
                    setOnClickListener { copyEventCorrelation(event) }
                })
            })
        }
    }

    private fun copyEventCorrelation(event: TestEvent) {
        val correlation = event.correlation
        val observation = event.observation
        val content = buildString {
            appendLine("Test Dialer — dane zdarzenia do korelacji")
            appendLine("Czas: ${formatDateWithMillis(event.occurredAtMillis)}")
            appendLine("Epoch ms: ${event.occurredAtMillis}")
            appendLine("Typ: ${event.action.serviceType.name}")
            appendLine("Event ID: ${event.id.value}")
            appendLine("Run ID: ${event.runId.value}")
            appendLine("Step ID: ${event.stepId.value}")
            eventParameters(event).forEach { appendLine(it) }
            appendLine("Źródło: ${correlation.sourceAddress.orEmpty()}")
            appendLine("Cel korelacji: ${correlation.destinationAddress.orEmpty()}")
            appendLine("Alias abonenta: ${correlation.subscriberAlias.orEmpty()}")
            correlation.references.forEach { appendLine("${it.namespace}: ${it.value}") }
            if (observation == null) appendLine("Obserwacja: brak") else {
                appendLine("Obserwacja: ${observation.status.name}")
                appendLine("Źródło obserwacji: ${observation.source.name}")
                appendLine("Kod obserwacji: ${observation.code}")
            }
            append("Ocena billingu: poza tym zestawem; wymaga osobnej weryfikacji")
        }
        runCatching {
            val clip = ClipData.newPlainText("Test Dialer — korelacja", content)
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                clip.description.extras = android.os.PersistableBundle().apply {
                    putBoolean(android.content.ClipDescription.EXTRA_IS_SENSITIVE, true)
                }
            }
            getSystemService(ClipboardManager::class.java).setPrimaryClip(clip)
        }.fold(
            onSuccess = { Toast.makeText(this, R.string.register_correlation_copied, Toast.LENGTH_SHORT).show() },
            onFailure = { Toast.makeText(this, R.string.register_correlation_copy_failed, Toast.LENGTH_LONG).show() },
        )
    }

    private fun repeatPreparationBlocked(): Boolean = activeRunState.active != null || activeRunState.busy ||
        activeRunViewModel.executionInProgress() || manualSessionState.active != null || manualSessionState.busy || isTestTypeSwitchLocked()

    private fun prepareSessionRepeat(sourceId: RunId) {
        if (repeatPreparationBlocked()) { templateTransferMessage("Najpierw zakończ bieżącą sesję lub test."); return }
        if (reportBusy) return
        reportBusy = true
        reportExecutor.execute {
            val result = runCatching {
                val stored = (application as TestDialerApplication).testRunRepository.get(sourceId) ?: error("Sesja jest niedostępna.")
                com.example.testdialer.active.SessionRepeatPlan.from(stored)
            }
            runOnUiThread {
                reportBusy = false
                if (!isDestroyed && !isFinishing) result.fold(::showRepeatPlanPreview) { templateTransferMessage(it.message ?: "Nie udało się przygotować powtórki") }
            }
        }
    }

    private fun showRepeatPlanPreview(plan: com.example.testdialer.active.LocalScenario) {
        val summary = plan.steps.joinToString("\n") { step -> step.title + if (step.action is TestAction.Data) {
            plan.dataAmounts[step.id]?.let { " · $it B" } ?: " · ilość do podania"
        } else "" }
        AlertDialog.Builder(this).setTitle("Plan powtórki")
            .setMessage("${plan.name}\n\n$summary\n\nNowa sesja zachowa parametry zapisanych zdarzeń. Wyniki, oceny i notatki pozostają w poprzedniej sesji. Każdą usługę uruchamiasz osobno z formularza.")
            .setPositiveButton("Przygotuj nową sesję") { _, _ ->
                if (repeatPreparationBlocked()) templateTransferMessage("Najpierw zakończ bieżącą sesję lub test.")
                else {
                    setExecutionFocus(false)
                    activeRunViewModel.startScenario(plan)
                    showSection(AppSection.TEST)
                }
            }.setNeutralButton("Zapisz plan") { _, _ -> saveScenarioPlan(plan) }
            .setNegativeButton("Anuluj", null).show()
    }

    private fun saveScenarioPlan(plan: com.example.testdialer.active.LocalScenario) {
        val input = createOptionalInput("Nazwa planu").apply {
            setText(plan.name); filters = arrayOf(android.text.InputFilter.LengthFilter(80))
        }
        val dialog = AlertDialog.Builder(this).setTitle("Zapisz plan sesji").setView(input)
            .setPositiveButton("Zapisz", null).setNegativeButton("Anuluj", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (input.text.isBlank()) { input.error = "Podaj nazwę"; return@setOnClickListener }
                val result = runCatching { check(scenarioPlans.save(input.text.toString(), plan)) }
                if (result.isSuccess) { dialog.dismiss(); templateTransferMessage("Plan sesji zapisany") }
                else input.error = result.exceptionOrNull()?.message ?: "Nie udało się zapisać planu"
            }
        }
        dialog.show()
    }

    private fun showScenarioPlans() {
        val items = scenarioPlans.list()
        if (items.isEmpty()) {
            AlertDialog.Builder(this).setTitle("Zapisane plany sesji")
                .setMessage("Plan możesz zapisać z podglądu powtórki historycznej sesji. Zapis zawiera tylko kroki i parametry, bez wyników, ocen oraz notatek.")
                .setPositiveButton("Importuj z pliku") { _, _ -> importPlansLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }
                .setNegativeButton("Zamknij", null).show()
            return
        }
        AlertDialog.Builder(this).setTitle("Zapisane plany sesji")
            .setItems(items.map { "${it.name} · ${it.scenario.steps.size} kroków" }.toTypedArray()) { _, index ->
                val saved = items[index]
                AlertDialog.Builder(this).setTitle(saved.name)
                    .setItems(arrayOf("Pokaż i przygotuj sesję", "Usuń plan")) { _, choice ->
                        if (choice == 0) {
                            if (repeatPreparationBlocked()) templateTransferMessage("Najpierw zakończ bieżącą sesję lub test.")
                            else runCatching { scenarioPlans.instantiate(saved.id) }.fold(::showRepeatPlanPreview) { templateTransferMessage(it.message ?: "Plan jest niedostępny") }
                        } else AlertDialog.Builder(this).setTitle("Usunąć plan ${saved.name}?")
                            .setMessage("Historia wykonanych sesji pozostanie zachowana.")
                            .setPositiveButton("Usuń") { _, _ -> templateTransferMessage(if (scenarioPlans.delete(saved.id)) "Plan usunięty" else "Nie udało się usunąć planu") }
                            .setNegativeButton("Anuluj", null).show()
                    }.setNegativeButton("Anuluj", null).show()
            }.setNeutralButton("Kopia / import") { _, _ -> showPlanBackup() }
            .setNegativeButton("Zamknij", null).show()
    }

    private fun showPlanBackup() {
        AlertDialog.Builder(this).setTitle("Plany sesji i kopia zapasowa")
            .setMessage("Kopia zawiera parametry wszystkich kroków, w tym numery i treści SMS. Import najpierw pokaże zawartość, doda tylko nowe plany i nie uruchomi testów.")
            .setPositiveButton("Zapisz kopię") { _, _ -> exportPlansLauncher.launch("test-dialer-plany.json") }
            .setNeutralButton("Importuj") { _, _ -> importPlansLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }
            .setNegativeButton("Zamknij", null).show()
    }

    private fun showHistoryBackup() {
        AlertDialog.Builder(this).setTitle("Historia i kopia zapasowa")
            .setMessage("Kopia zawiera pełne sesje, parametry, obserwacje, notatki i osobno oznaczone ręczne oceny billingu. Może zawierać numery, treści SMS oraz identyfikatory korelacji. Import najpierw pokazuje podgląd i wymaga osobnego potwierdzenia.")
            .setPositiveButton("Zapisz kopię") { _, _ -> exportHistoryLauncher.launch("test-dialer-historia.json") }
            .setNeutralButton("Wczytaj kopię") { _, _ -> previewHistoryLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }
            .setNegativeButton("Zamknij", null).show()
    }

    private fun showHistoryImportPreview(entries: List<com.example.testdialer.report.HistoryArchiveEntry>) {
        val events = entries.sumOf { it.stored.run.events.size }
        val observations = entries.sumOf { entry -> entry.stored.run.events.count { it.observation != null } }
        val reviews = entries.sumOf { it.reviews.size }
        val notes = entries.count { it.testerNote.isNotBlank() }
        val running = entries.count { it.stored.run.status == TestRunStatus.RUNNING || it.stored.run.status == TestRunStatus.CREATED }
        val examples = entries.take(6).joinToString("\n") { "${it.stored.scenario.name} · ${it.stored.run.events.size} zdarzeń" }
        val message = "Sesje: ${entries.size}. Zdarzenia: $events. Obserwacje techniczne: $observations. Ręczne oceny billingu: $reviews. Notatki: $notes." +
            (if (running > 0) "\nSesje niezakończone w pliku: $running. Pozostaną wyłącznie historią i nie zostaną wznowione." else "") +
            (if (examples.isBlank()) "\n\nPlik nie zawiera sesji." else "\n\n$examples" + if (entries.size > 6) "\n… i pozostałe ${entries.size - 6}" else "") +
            "\n\nIstniejące sesje, notatki i oceny nie zostaną nadpisane. Konflikt przerwie cały import. Import nie uruchamia połączeń, SMS ani transferu danych."
        val builder = AlertDialog.Builder(this).setTitle("Podgląd kopii historii")
            .setMessage(message)
        if (entries.isEmpty()) {
            builder.setPositiveButton("Zamknij", null).show()
        } else {
            builder.setPositiveButton("Przywróć historię") { _, _ -> restoreHistory(entries) }
                .setNegativeButton("Anuluj", null).show()
        }
    }

    private fun restoreHistory(entries: List<com.example.testdialer.report.HistoryArchiveEntry>) {
        templateTransferMessage("Przywracam historię. Nie zamykaj aplikacji.")
        reportExecutor.execute {
            val result = runCatching {
                (application as TestDialerApplication).annotationStore.restoreHistory(entries)
            }
            runOnUiThread {
                if (!isDestroyed && !isFinishing) {
                    result.fold({ restored ->
                        registerViewModel.load()
                        renderRegister()
                        templateTransferMessage(
                            "Historia przywrócona. Dodano sesje: ${restored.addedRuns}. " +
                                "Już istniejące: ${restored.existingRuns}. " +
                                "Uzupełniono notatki: ${restored.annotations.notes}, oceny: ${restored.annotations.reviews}.",
                        )
                    }, { error ->
                        val detail = if (error is com.example.testdialer.persistence.SnapshotConflictException)
                            "Kopia różni się od historii zapisanej w telefonie. Niczego nie zmieniono."
                        else "Nie udało się przywrócić historii: ${error.message}. Niczego nie zmieniono."
                        templateTransferMessage(detail)
                    })
                }
            }
        }
    }

    private fun confirmPlanImport(items: List<com.example.testdialer.active.SavedScenarioPlan>) {
        val preview = runCatching { scenarioPlans.previewImport(items) }
        if (preview.isFailure) { templateTransferMessage(preview.exceptionOrNull()?.message ?: "Nieprawidłowy plik"); return }
        val counts = preview.getOrThrow()
        if (counts.added == 0) { templateTransferMessage("Brak nowych planów. Pominięto powtórzenia: ${counts.skipped}."); return }
        val descriptions = items.take(8).joinToString("\n") { "${it.name} · ${it.scenario.steps.size} kroków" }
        AlertDialog.Builder(this).setTitle("Podgląd importu planów")
            .setMessage("Nowych: ${counts.added}. Powtórzenia pominięte: ${counts.skipped}.\n\n$descriptions" +
                (if (items.size > 8) "\n… i pozostałe ${items.size - 8}" else "") +
                "\n\nIstniejące plany i historia pozostaną zachowane. Import nie uruchomi usług.")
            .setPositiveButton("Dodaj plany") { _, _ -> reportExecutor.execute {
                val result = runCatching { scenarioPlans.importItems(items) }
                templateTransferMessage(result.fold({ "Dodano plany: ${it.added}. Pominięto powtórzenia: ${it.skipped}." }, { "Import planów nie powiódł się: ${it.message}" }))
            } }.setNegativeButton("Anuluj", null).show()
    }

    private fun chooseComparison(baselineId: RunId) {
        val candidates = registerState.runs.filter { it.runId != baselineId }
        if (candidates.isEmpty()) { templateTransferMessage("Zapisz co najmniej dwie sesje, aby porównać wyniki."); return }
        AlertDialog.Builder(this).setTitle("Wybierz sesję do porównania")
            .setItems(candidates.map { "${it.scenarioName} · ${formatDateWithMillis(it.startedAtMillis)} · ${it.eventCount} zdarzeń" }.toTypedArray()) { _, index ->
                if (reportBusy) return@setItems
                reportBusy = true
                templateTransferMessage("Wczytuję porównanie sesji")
                reportExecutor.execute {
                    val result = runCatching {
                        val repository = (application as TestDialerApplication).testRunRepository
                        val a = repository.get(baselineId) ?: error("Sesja bazowa jest niedostępna.")
                        val b = repository.get(candidates[index].runId) ?: error("Wybrana sesja jest niedostępna.")
                        val reviews = (a.run.events + b.run.events).associate { it.id.value to billingReviews.get(it.id) }
                        com.example.testdialer.report.RunComparisonFormatter.compare(a, b, reviews)
                    }
                    runOnUiThread {
                        reportBusy = false
                        if (!isDestroyed && !isFinishing) result.fold(::showComparison) { templateTransferMessage("Nie udało się porównać: ${it.message}") }
                    }
                }
            }.setNegativeButton("Anuluj", null).show()
    }

    private fun showComparison(comparison: com.example.testdialer.report.RunComparison) {
        var onlyDifferences = false
        val comparisonText = createBodyText(comparison.text()).apply { setTextIsSelectable(true) }
        val fields = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dimen(20), dimen(8), dimen(20), dimen(16))
            addView(Button(this@MainActivity).apply {
                text = "Pokaż tylko różnice"; isAllCaps = false; minHeight = dimen(48)
                setOnClickListener {
                    onlyDifferences = !onlyDifferences
                    this.text = if (onlyDifferences) "Pokaż wszystkie pola" else "Pokaż tylko różnice"
                    comparisonText.text = comparison.text(onlyDifferences)
                    announceForAccessibility(if (onlyDifferences) "Wyświetlono różnice: ${comparison.differences.size}" else "Wyświetlono wszystkie pola")
                }
            })
            addView(Button(this@MainActivity).apply {
                this.text = "Odczytaj podsumowanie"; isAllCaps = false; minHeight = dimen(48)
                setOnClickListener {
                    if (!getSharedPreferences("ui-settings", MODE_PRIVATE).getBoolean("speech", false)) templateTransferMessage("Najpierw włącz komunikaty głosowe w Ustawieniach")
                    else speech.say("Porównanie sesji. Różnice w ${comparison.differences.size} polach. " + when (comparison.parametersMatch) { true -> "Zapisane parametry usług zgodne. Oceny rozliczenia są ręcznymi ocenami testera."; false -> "Parametry usług są różne. Różnice wyników nie oznaczają automatycznie regresji."; null -> "Brak pełnych parametrów do sprawdzenia zgodności." })
                }
            })
            addView(comparisonText)
        }
        AlertDialog.Builder(this).setTitle("Porównanie sesji")
            .setView(ScrollView(this).apply { addView(fields) })
            .setPositiveButton("Udostępnij TXT") { _, _ ->
                val snapshot = comparison.text(onlyDifferences)
                reportExecutor.execute {
                    val result = runCatching { RunReportFiles.shareIntent(this, snapshot, "txt") }
                    runOnUiThread { if (!isDestroyed && !isFinishing) result.fold({ startActivity(Intent.createChooser(it, "Porównanie sesji")) }, { templateTransferMessage("Nie udało się udostępnić: ${it.message}") }) }
                }
            }.setNegativeButton("Zamknij", null).show()
    }

    private fun dataBytes(run: com.example.testdialer.domain.TestRun): String = run.events
        .filter { it.action is TestAction.Data }
        .sumOf { event -> event.correlation.references.firstOrNull { it.namespace == "bytes" }?.value?.toLongOrNull()?.coerceAtLeast(0) ?: 0L }
        .let { String.format(Locale.getDefault(), "%,d B (%.3f MB)", it, it / 1_000_000.0) }

    private fun withAnnotationsReady(action: () -> Unit) {
        val store = (application as TestDialerApplication).annotationStore
        if (store.ready()) { action(); return }
        reportExecutor.execute {
            val ready = store.initialize()
            runOnUiThread {
                if (!isDestroyed && !isFinishing) {
                    if (ready) action()
                    else Toast.makeText(this, "Nie można teraz zapisać adnotacji. Stara kopia pozostaje zachowana.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun editRunNote(runId: RunId) = withAnnotationsReady { showRunNoteEditor(runId) }

    private fun showRunNoteEditor(runId: RunId) {
        val input = EditText(this).apply {
            setText(runNotes.get(runId))
            hint = getString(R.string.run_note_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            minLines = 5
            maxLines = 10
            gravity = Gravity.TOP
            filters = arrayOf(android.text.InputFilter.LengthFilter(com.example.testdialer.notes.RunNotesStore.MAX_LENGTH))
            setPadding(dimen(18), dimen(12), dimen(18), dimen(12))
        }
        AlertDialog.Builder(this).setTitle(R.string.run_note).setView(input)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.run_note_save) { _, _ ->
                val text = input.text.toString()
                reportExecutor.execute {
                    val saved = runCatching { runNotes.save(runId, text) }.getOrDefault(false)
                    runOnUiThread {
                        if (!isDestroyed && !isFinishing) {
                            Toast.makeText(this, if (saved) R.string.run_note_saved else R.string.run_note_failed, Toast.LENGTH_LONG).show()
                            renderRegister()
                        }
                    }
                }
            }.show()
    }

    private fun saveTemplate(event: TestEvent) {
        val input = createOptionalInput("Nazwa szablonu").apply {
            setText(eventTypeLabel(event))
            filters = arrayOf(android.text.InputFilter.LengthFilter(80))
        }
        val dialog = AlertDialog.Builder(this).setTitle("Zapisz parametry jako szablon")
            .setView(input).setPositiveButton("Zapisz", null).setNegativeButton("Anuluj", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (input.text.isBlank()) { input.error = "Podaj nazwę"; return@setOnClickListener }
                val name = input.text.toString()
                val bytes = event.correlation.references.firstOrNull { it.namespace == "requestedBytes" }?.value?.toLongOrNull()
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                reportExecutor.execute {
                    val result = runCatching { check(templates.save(name, event.action, bytes)) { "Nie udało się zapisać szablonu" } }
                    runOnUiThread {
                        if (isDestroyed || isFinishing) return@runOnUiThread
                        if (result.isSuccess) { dialog.dismiss(); Toast.makeText(this, "Szablon zapisany", Toast.LENGTH_SHORT).show() }
                        else { dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true; input.error = result.exceptionOrNull()?.message }
                    }
                }
            }
        }
        dialog.show()
    }

    private fun showTemplates() {
        val items = templates.list()
        if (items.isEmpty()) {
            AlertDialog.Builder(this).setTitle("Szablony testów")
                .setMessage("Otwórz wykonane zdarzenie w Rejestrze i wybierz Zapisz jako szablon. Szablon zachowa numer, treść SMS lub ilość danych.")
                .setPositiveButton("Importuj z pliku") { _, _ -> importTemplatesLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }
                .setNegativeButton("Zamknij", null).show()
            return
        }
        AlertDialog.Builder(this).setTitle("Wybierz szablon")
            .setItems(items.map { it.name + " · " + when (it.action.serviceType) { ServiceType.VOICE -> "Połączenie"; ServiceType.SMS -> "SMS"; ServiceType.DATA -> "Dane" } }.toTypedArray()) { _, index ->
                val template = items[index]
                AlertDialog.Builder(this).setTitle(template.name).setItems(arrayOf("Użyj parametrów", "Usuń szablon")) { _, choice ->
                    if (choice == 0) {
                        if (activeRunState.busy || activeRunViewModel.executionInProgress() || isTestTypeSwitchLocked()) {
                            Toast.makeText(this, R.string.test_already_in_progress, Toast.LENGTH_LONG).show()
                        } else if (activeRunState.active == null) {
                            pendingTemplateId = template.id
                            activeRunViewModel.startEmpty(template.name)
                        } else applyTemplate(template)
                    } else AlertDialog.Builder(this).setTitle("Usunąć szablon ${template.name}?")
                        .setMessage("Wyniki wykonanych testów pozostaną zachowane.")
                        .setPositiveButton("Usuń") { _, _ -> reportExecutor.execute {
                            val success = templates.delete(template.id)
                            runOnUiThread { if (!isDestroyed && !isFinishing) Toast.makeText(this, if (success) "Szablon usunięty" else "Nie udało się usunąć szablonu", Toast.LENGTH_SHORT).show() }
                        } }.setNegativeButton("Anuluj", null).show()
                }.setNegativeButton("Anuluj", null).show()
            }.setNeutralButton("Kopia / import") { _, _ -> showTemplateBackup() }.setNegativeButton("Zamknij", null).show()
    }

    private fun showTemplateBackup() {
        AlertDialog.Builder(this).setTitle("Szablony i kopia zapasowa")
            .setMessage("Kopia zawiera parametry testów, w tym numery i treść SMS. Zapisz ją w wybranym miejscu. Import dodaje szablony po podglądzie; nie uruchamia testów.")
            .setPositiveButton("Zapisz kopię") { _, _ -> exportTemplatesLauncher.launch("test-dialer-szablony.json") }
            .setNeutralButton("Importuj") { _, _ -> importTemplatesLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }
            .setNegativeButton("Zamknij", null).show()
    }

    private fun confirmTemplateImport(items: List<com.example.testdialer.templates.TestTemplate>) {
        val preview = runCatching { templates.previewImport(items) }
        if (preview.isFailure) { templateTransferMessage(preview.exceptionOrNull()?.message ?: "Nieprawidłowy plik"); return }
        val counts = preview.getOrThrow()
        if (counts.added == 0) { templateTransferMessage("Brak nowych szablonów. Pominięto powtórzenia: ${counts.skipped}."); return }
        val descriptions = items.take(10).joinToString("\n") { it.name }
        AlertDialog.Builder(this).setTitle("Podgląd importu")
            .setMessage("Nowych: ${counts.added}. Powtórzenia pominięte: ${counts.skipped}.\n\n$descriptions" +
                (if (items.size > 10) "\n… i pozostałe ${items.size - 10}" else "") + "\n\nIstniejące szablony i historia pozostaną zachowane.")
            .setPositiveButton("Dodaj szablony") { _, _ -> reportExecutor.execute {
                val result = runCatching { templates.importItems(items) }
                templateTransferMessage(result.fold({ "Dodano: ${it.added}. Pominięto powtórzenia: ${it.skipped}." }, { "Import nie powiódł się: ${it.message}" }))
            } }.setNegativeButton("Anuluj", null).show()
    }

    private fun templateTransferMessage(message: String) {
        runOnUiThread { if (!isDestroyed && !isFinishing) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            if (lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) speech.say(message)
        } }
    }

    private fun applyTemplate(template: com.example.testdialer.templates.TestTemplate) {
        if (template.action is TestAction.Data) {
            dataAmountDraft = (template.requestedBytes ?: 1_000_000L).toString()
            dataUnitDraft = "B"
        }
        openActiveTask(null, template.action)
        showSection(AppSection.TEST)
    }

    private fun billingVerdictLabel(verdict: com.example.testdialer.review.BillingVerdict): String = when (verdict) {
        com.example.testdialer.review.BillingVerdict.NOT_CHECKED -> "nie sprawdzono"
        com.example.testdialer.review.BillingVerdict.PASS -> "zgodne (PASS)"
        com.example.testdialer.review.BillingVerdict.FAIL -> "niezgodne (FAIL)"
    }

    private fun editBillingReview(eventId: EventId) = withAnnotationsReady { showBillingReviewEditor(eventId) }

    private fun showBillingReviewEditor(eventId: EventId) {
        val before = billingReviews.get(eventId)
        val expected = createOptionalInput("Oczekiwane naliczenie, np. 0,79 PLN").apply { setText(before.expected); filters = arrayOf(android.text.InputFilter.LengthFilter(2000)) }
        val actual = createOptionalInput("Rzeczywiste naliczenie / dowód, np. CDR").apply { setText(before.actual); filters = arrayOf(android.text.InputFilter.LengthFilter(2000)) }
        val verdicts = com.example.testdialer.review.BillingVerdict.entries
        val choice = android.widget.Spinner(this).apply {
            adapter = android.widget.ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, verdicts.map(::billingVerdictLabel))
            setSelection(verdicts.indexOf(before.verdict))
            minimumHeight = dimen(48)
            contentDescription = "Ocena poprawności rozliczenia przez testera"
        }
        val fields = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dimen(20), dimen(8), dimen(20), dimen(8))
            addView(createBodyText("To ręczna ocena rozliczenia. Nie zmienia technicznego wyniku usługi."))
            addView(expected); addView(actual); addView(choice)
        }
        val scroll = ScrollView(this).apply { addView(fields) }
        val dialog = AlertDialog.Builder(this).setTitle("Oczekiwano / otrzymano").setView(scroll)
            .setPositiveButton("Zapisz", null).setNegativeButton("Anuluj", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val review = com.example.testdialer.review.BillingReview(expected.text.toString(), actual.text.toString(), verdicts[choice.selectedItemPosition], System.currentTimeMillis())
                if (review.verdict != com.example.testdialer.review.BillingVerdict.NOT_CHECKED && (review.expected.isBlank() || review.actual.isBlank())) {
                    actual.error = "Opisz oczekiwanie i otrzymany wynik, aby zapisać PASS/FAIL"; return@setOnClickListener
                }
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                reportExecutor.execute {
                    val result = runCatching { check(billingReviews.save(eventId, review)) }
                    runOnUiThread {
                        if (isDestroyed || isFinishing) return@runOnUiThread
                        if (result.isSuccess) { dialog.dismiss(); renderRegister() }
                        else { dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true; actual.error = "Nie udało się zapisać oceny" }
                    }
                }
            }
        }
        dialog.show()
    }

    private fun startDataScenario() {
        if (activeRunState.active != null || activeRunState.busy || activeRunViewModel.executionInProgress()) return
        activeRunViewModel.startScenario(LocalScenarioCatalog.dataQuota())
    }

    private fun reportButton(label: String, action: () -> Unit): Button = Button(this).apply {
        text = label
        isAllCaps = false
        textSize = 16f
        minHeight = dimen(50)
        setPadding(dimen(14), dimen(10), dimen(14), dimen(10))
        background = pillBackground(ColorPalette.accent)
        setTextColor(ColorPalette.onAccent)
        setOnClickListener { action() }
    }

    private fun showReportOptions(stored: StoredTestRun) {
        AlertDialog.Builder(this)
            .setTitle(R.string.report_export)
            .setItems(arrayOf(getString(R.string.report_copy), getString(R.string.report_share_text), getString(R.string.report_share_json), "Udostępnij CSV")) { _, option ->
                exportReport(stored, option)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun exportReport(stored: StoredTestRun, option: Int) {
        if (reportBusy) return
        reportBusy = true
        Toast.makeText(this, R.string.report_preparing, Toast.LENGTH_SHORT).show()
        val appContext = applicationContext
        reportExecutor.execute {
            val note = runNotes.get(stored.run.id)
            val reviews = stored.run.events.associate { it.id.value to billingReviews.get(it.id) }.filterValues { it.reviewedAtMillis > 0 }
            val interruptedAt = billingReviews.interruptedAt(stored.run.id)
            val result = runCatching {
                val content = when (option) {
                    2 -> RunReportFormatter.json(stored, note, reviews, interruptedAt)
                    3 -> RunReportFormatter.csv(stored, note, reviews, interruptedAt)
                    else -> RunReportFormatter.text(stored, note, reviews, interruptedAt)
                }
                if (option == 0) {
                    require(content.toByteArray(Charsets.UTF_8).size <= 100_000) { "clipboard_limit" }
                    content to null
                } else content to RunReportFiles.shareIntent(appContext, content, when (option) { 2 -> "json"; 3 -> "csv"; else -> "txt" })
            }
            runOnUiThread {
                reportBusy = false
                if (isDestroyed || isFinishing) return@runOnUiThread
                result.fold(onSuccess = { (content, intent) ->
                    runCatching {
                        if (intent == null) {
                            val clip = ClipData.newPlainText("Test Dialer", content)
                            if (android.os.Build.VERSION.SDK_INT >= 33) {
                                clip.description.extras = android.os.PersistableBundle().apply {
                                    putBoolean(android.content.ClipDescription.EXTRA_IS_SENSITIVE, true)
                                }
                            }
                            getSystemService(ClipboardManager::class.java).setPrimaryClip(clip)
                            Toast.makeText(this, R.string.report_copied, Toast.LENGTH_SHORT).show()
                        } else startActivity(Intent.createChooser(intent, getString(R.string.report_export)))
                    }.onFailure { Toast.makeText(this, R.string.report_failed, Toast.LENGTH_LONG).show() }
                }, onFailure = {
                    Toast.makeText(this, if (it.message == "clipboard_limit") R.string.report_too_large else R.string.report_failed, Toast.LENGTH_LONG).show()
                })
            }
        }
    }

    private fun eventParameters(event: TestEvent): List<String> = when (val action = event.action) {
        is TestAction.Voice -> listOf(getString(R.string.register_parameter_destination, action.destination))
        is TestAction.Sms -> buildList {
            add(getString(R.string.register_parameter_destination, action.destination))
            add(getString(R.string.register_parameter_message, action.message ?: getString(R.string.register_value_missing)))
        }
        is TestAction.Data -> listOf(getString(R.string.register_parameter_target, action.target))
    }

    private fun eventTypeLabel(event: TestEvent): String = when (event.action.serviceType) {
        ServiceType.VOICE -> getString(R.string.voice_type)
        ServiceType.SMS -> getString(R.string.sms_type)
        ServiceType.DATA -> getString(R.string.data_type)
    }

    private fun observationLabel(status: ObservationStatus): String = when (status) {
        ObservationStatus.CONFIRMED -> getString(R.string.register_observation_confirmed)
        ObservationStatus.NOT_CONFIRMED -> getString(R.string.register_observation_not_confirmed)
        ObservationStatus.NOT_VERIFIED -> getString(R.string.register_observation_not_verified)
    }

    private fun createManualSummaryCard(summary: TestRunSummary): View {
        val date = formatDate(summary.startedAtMillis)
        val status = localizeRunStatus(summary.status)
        return createCard {
            isClickable = true
            isFocusable = true
            minimumHeight = dimen(64)
            contentDescription = getString(
                R.string.manual_history_item_accessibility,
                summary.scenarioName,
                status,
                date,
            )
            addView(createCardTitle(summary.scenarioName))
            addView(spaceVertical(dimen(6)))
            addView(createBodyText(getString(R.string.manual_history_item, status, date)))
            if (summary.status == TestRunStatus.RUNNING) {
                addView(spaceVertical(dimen(6)))
                addView(createStatusText(getString(R.string.manual_history_not_resumable)))
            }
            setOnClickListener { manualSessionViewModel.select(summary.runId) }
        }
    }

    private fun createManualSessionDetail(stored: com.example.testdialer.persistence.StoredTestRun): View {
        val scenario = stored.scenario
        val run = stored.run
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(Button(this@MainActivity).apply {
                setText(R.string.manual_detail_back)
                isAllCaps = false
                minHeight = dimen(48)
                setOnClickListener { manualSessionViewModel.clearSelection() }
            })
            addView(spaceVertical(dimen(12)))
            addView(createCard {
                addView(createHeaderText(scenario.name).apply {
                    ViewCompat.setAccessibilityHeading(this, true)
                })
                addView(spaceVertical(dimen(8)))
                addView(createBodyText(getString(
                    R.string.manual_detail_status,
                    localizeRunStatus(run.status),
                )))
                addView(spaceVertical(dimen(6)))
                addView(createBodyText(getString(R.string.manual_detail_started, formatDate(run.startedAtMillis))))
                run.completedAtMillis?.let {
                    addView(spaceVertical(dimen(6)))
                    addView(createBodyText(getString(R.string.manual_detail_completed, formatDate(it))))
                }
                addView(spaceVertical(dimen(10)))
                addView(createMicroText(getString(R.string.manual_session_run_id, run.id.value)).apply {
                    setTextIsSelectable(true)
                })
            })
            addView(spaceVertical(dimen(12)))
            addView(createCard {
                addView(createCardTitle(getString(R.string.manual_detail_timeline)).apply {
                    ViewCompat.setAccessibilityHeading(this, true)
                })
                run.timeline.forEach { entry ->
                    addView(spaceVertical(dimen(10)))
                    addView(createBodyText(getString(
                        R.string.manual_detail_timeline_item,
                        entry.sequenceNumber + 1,
                        localizeTimelineKind(entry.kind),
                        formatDateWithMillis(entry.capturedAt.epochMillis),
                    )))
                    entry.relatedEventId?.let { id ->
                        addView(createMicroText(getString(R.string.manual_detail_event_id, id.value)).apply {
                            setTextIsSelectable(true)
                        })
                    }
                }
            })
        }
    }

    private fun localizeRunStatus(status: TestRunStatus): String = when (status) {
        TestRunStatus.CREATED -> getString(R.string.manual_status_created)
        TestRunStatus.RUNNING -> getString(R.string.manual_status_running)
        TestRunStatus.COMPLETED -> getString(R.string.manual_status_completed)
        TestRunStatus.ABORTED -> getString(R.string.manual_status_aborted)
    }

    private fun localizeTimelineKind(kind: TimelineEntryKind): String = when (kind) {
        TimelineEntryKind.RUN_STARTED -> getString(R.string.timeline_run_started)
        TimelineEntryKind.STEP_STARTED -> getString(R.string.timeline_step_started)
        TimelineEntryKind.ATTEMPT_STARTED -> getString(R.string.timeline_attempt_started)
        TimelineEntryKind.ACTION_RECORDED -> getString(R.string.timeline_action_recorded)
        TimelineEntryKind.ATTEMPT_FINISHED -> getString(R.string.timeline_attempt_finished)
        TimelineEntryKind.STEP_FINISHED -> getString(R.string.timeline_step_finished)
        TimelineEntryKind.RUN_COMPLETED -> getString(R.string.timeline_run_completed)
        TimelineEntryKind.RUN_ABORTED -> getString(R.string.timeline_run_aborted)
    }

    private fun formatDate(millis: Long): String =
        SimpleDateFormat(getString(R.string.result_date_pattern), Locale.getDefault()).format(Date(millis))

    private fun formatDateWithMillis(millis: Long): String =
        SimpleDateFormat(getString(R.string.manual_detail_date_pattern), Locale.getDefault()).format(Date(millis))

    private fun createVoiceResultCard(result: VoiceTestResult): View {
        val outcomeLabel = when (result.outcome) {
            VoiceTestResult.Outcome.SUCCESS -> getString(R.string.outcome_success)
            VoiceTestResult.Outcome.FAILURE -> getString(R.string.outcome_failure)
            VoiceTestResult.Outcome.NOT_CHECKED -> getString(R.string.outcome_not_checked)
        }
        val outcomeColor = when (result.outcome) {
            VoiceTestResult.Outcome.SUCCESS -> ColorPalette.ok
            VoiceTestResult.Outcome.FAILURE -> ColorPalette.bad
            VoiceTestResult.Outcome.NOT_CHECKED -> ColorPalette.neutral
        }
        val formattedDate = SimpleDateFormat(getString(R.string.result_date_pattern), Locale.getDefault())
            .format(Date(result.timestampMillis))
        return createCard {
            elevation = 0f
            setPadding(dimen(14), dimen(14), dimen(14), dimen(14))
            contentDescription = buildString {
                append(getString(R.string.result_accessibility, outcomeLabel, formattedDate, result.phoneNumber))
                result.testName?.let { append(getString(R.string.result_accessibility_name, it)) }
            }
            addView(TextView(this@MainActivity).apply {
                text = outcomeLabel
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(ColorPalette.onAccent)
                setPadding(dimen(10), dimen(6), dimen(10), dimen(6))
                background = pillBackground(outcomeColor)
            })
            addView(spaceVertical(dimen(10)))
            addView(createStatusText(result.testName ?: getString(R.string.result_unnamed)).apply {
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
            })
            addView(spaceVertical(dimen(6)))
            addView(createBodyText(getString(R.string.result_type_value)))
            addView(spaceVertical(dimen(4)))
            addView(createBodyText(getString(R.string.result_phone_value, result.phoneNumber)))
            addView(spaceVertical(dimen(4)))
            addView(createBodyText(getString(R.string.result_date_value, formattedDate)))
        }
    }

    private fun createTestTypeSelectorCard(): View {
        return createCard {
            addView(createCardTitle(getString(R.string.test_selector_title)).apply {
                ViewCompat.setAccessibilityHeading(this, true)
            })
            addView(spaceVertical(dimen(12)))
            val row = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            row.addView(createTestTypeChip(TestType.VOICE, getString(R.string.voice_type)))
            row.addView(spaceHorizontal(dimen(8)))
            row.addView(createTestTypeChip(TestType.SMS, getString(R.string.sms_type)))
            row.addView(spaceHorizontal(dimen(8)))
            row.addView(createTestTypeChip(TestType.DATA, getString(R.string.data_type)))
            addView(row)
        }
    }

    private fun createTestTypeChip(type: TestType, label: String): Button {
        return Button(this@MainActivity).apply {
            text = label
            isAllCaps = false
            textSize = 14f
            minHeight = dimen(48)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                if (isTestTypeSwitchLocked()) return@setOnClickListener
                selectedActiveTaskId = null
                clearDraft(type)
                currentTestType = type
                updateTestTypeChips()
                renderScenario(type)
            }
            testTypeButtons[type] = this
        }
    }

    private fun updateTestTypeChips() {
        val switchLocked = isTestTypeSwitchLocked()
        testTypeButtons.forEach { (type, button) ->
            val selected = type == currentTestType
            button.isSelected = selected
            button.isEnabled = !switchLocked || selected
            button.alpha = if (selected) 1f else 0.92f
            button.background = pillBackground(if (selected) ColorPalette.accent else ColorPalette.button)
            button.setTextColor(if (selected) ColorPalette.onAccent else ColorPalette.textPrimary)
        }
    }

    private fun isTestTypeSwitchLocked(): Boolean =
        awaitingVoiceOutcome ||
            dialerWasOpened ||
            guidedSmsState.busy ||
            guidedSmsState.composerRequested ||
            guidedSmsState.composerOpen ||
            guidedSmsState.awaitingObservation ||
            cellularDataState.busy

    private fun renderScenario(type: TestType) {
        if (!::testScenarioHost.isInitialized) return
        testScenarioHost.removeAllViews()
        renderExecutionContext(type)
        when (type) {
            TestType.VOICE -> testScenarioHost.addView(createVoiceScenario())
            TestType.SMS -> testScenarioHost.addView(createGuidedSmsScenario())
            TestType.DATA -> testScenarioHost.addView(createCellularDataScenario())
        }
        updateTestTypeChips()
    }

    private fun renderExecutionContext(type: TestType) {
        runHomeView.executionContextHost.removeAllViews()
        val active = activeRunState.active ?: return
        val task = active.tasks.singleOrNull { it.step.id == selectedActiveTaskId }
        val stage = when {
            type == TestType.VOICE && awaitingVoiceOutcome -> getString(R.string.execution_stage_observation)
            type == TestType.VOICE && resultSaved -> getString(R.string.execution_stage_saved)
            type == TestType.SMS && guidedSmsState.awaitingObservation -> getString(R.string.execution_stage_observation)
            type == TestType.SMS && guidedSmsState.saved -> getString(R.string.execution_stage_saved)
            type == TestType.DATA && cellularDataState.busy -> getString(R.string.execution_stage_running)
            type == TestType.DATA && cellularDataState.saved -> getString(R.string.execution_stage_saved)
            else -> getString(R.string.execution_stage_prepare)
        }
        val taskName = task?.step?.title ?: getString(R.string.execution_manual_test)
        runHomeView.executionContextHost.addView(createCard {
            contentDescription = getString(
                R.string.execution_context_accessibility,
                active.stored.scenario.name,
                active.stored.run.id.value,
                taskName,
                stage,
            )
            addView(createBodyText("Sesja: ${active.stored.scenario.name}"))
            addView(spaceVertical(dimen(4)))
            addView(createStatusText(getString(R.string.execution_task_stage, taskName, stage)))
        })
    }

    private fun clearDraft(type: TestType) {
        when (type) {
            TestType.VOICE -> { voiceNameDraft = null; voicePhoneDraft = null }
            TestType.SMS -> { smsLabelDraft = null; smsDestinationDraft = null; smsMessageDraft = null }
            TestType.DATA -> { dataLabelDraft = null; dataUrlDraft = null }
        }
    }

    private fun EditText.trackDraft(onChanged: (String) -> Unit) {
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) =
                onChanged(s?.toString().orEmpty())
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun optionalFields(label: String, content: LinearLayout.() -> Unit): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val fields = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                visibility = View.GONE
                content()
            }
            addView(Button(this@MainActivity).apply {
                text = label
                isAllCaps = false
                minHeight = dimen(48)
                contentDescription = label
                ViewCompat.setStateDescription(this, getString(R.string.execution_optional_collapsed))
                setOnClickListener {
                    fields.visibility = if (fields.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                    isSelected = fields.visibility == View.VISIBLE
                    val expanded = fields.visibility == View.VISIBLE
                    text = getString(if (expanded) R.string.execution_optional_hide else R.string.execution_optional_details)
                    contentDescription = text
                    ViewCompat.setStateDescription(
                        this,
                        getString(if (expanded) R.string.execution_optional_expanded else R.string.execution_optional_collapsed),
                    )
                }
            })
            addView(fields)
        }

    private fun createCellularDataScenario(): View {
        val state = cellularDataState
        if (state.busy) return createCard {
            addView(createCardTitle(getString(R.string.data_running)))
            addView(spaceVertical(dimen(12)))
            addView(createBodyText(getString(R.string.data_progress, state.bytes, state.targetBytes)))
            addView(spaceVertical(dimen(10)))
            addView(ProgressBar(this@MainActivity, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = 1000
                progress = if (state.targetBytes > 0) (state.bytes * 1000 / state.targetBytes).toInt() else 0
                progressTintList = ColorStateList.valueOf(ColorPalette.accent)
                contentDescription = getString(R.string.data_progress, state.bytes, state.targetBytes)
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dimen(12))
            })
            addView(spaceVertical(dimen(10)))
            val speed = if (state.elapsedMillis > 0) state.bytes / 1000.0 / state.elapsedMillis else 0.0
            addView(createBodyText(getString(R.string.data_speed, state.elapsedMillis / 1000, speed)))
            addView(spaceVertical(dimen(12)))
            addView(reportButton(getString(R.string.data_cancel)) { cellularDataViewModel.cancel() })
            addView(spaceVertical(dimen(8)))
            addView(createStatusText(getString(R.string.data_foreground)))
        }
        if (state.saved) return createCard {
            addView(createCardTitle(getString(R.string.data_saved_title)))
            addView(spaceVertical(dimen(8)))
            addView(createBodyText(if (state.cancelled) getString(R.string.data_cancelled) else getString(R.string.data_saved_description)))
            state.completed?.run?.events?.lastOrNull()?.let { event ->
                val refs = event.correlation.references.associate { it.namespace to it.value }
                addView(spaceVertical(dimen(8)))
                addView(createBodyText(getString(R.string.data_result_detail, refs["bytes"] ?: "0", refs["requestedBytes"] ?: "—", refs["resultCode"] ?: "—")))
            }
            addView(spaceVertical(dimen(14)))
            addView(Button(this@MainActivity).apply {
                setText(R.string.go_to_register)
                isAllCaps = false
                minHeight = dimen(52)
                setOnClickListener { showSection(AppSection.REGISTER) }
            })
            addView(spaceVertical(dimen(10)))
            addView(Button(this@MainActivity).apply {
                setText(R.string.data_start_another)
                isAllCaps = false
                minHeight = dimen(52)
                setOnClickListener { cellularDataViewModel.startAnother() }
            })
        }
        return createCard {
            addView(createCardTitle(getString(R.string.data_card_title)))
            addView(spaceVertical(dimen(8)))
            addView(createBodyText(getString(R.string.data_card_description)))
            addView(spaceVertical(dimen(14)))
            val label = createOptionalInput(getString(R.string.data_label_hint)).apply {
                setText(dataLabelDraft.orEmpty())
                trackDraft { dataLabelDraft = it }
            }
            val url = createOptionalInput(getString(R.string.data_url_hint)).apply {
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
                setText(dataUrlDraft ?: (selectedTaskAction() as? TestAction.Data)?.target ?: com.example.testdialer.data.DataVolume.DEFAULT_URL)
                trackDraft { dataUrlDraft = it }
            }
            val amount = createOptionalInput(getString(R.string.data_amount_hint)).apply {
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                setText(dataAmountDraft)
                trackDraft { dataAmountDraft = it }
            }
            val presets = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }
            listOf(1, 100, 500).forEach { megabytes ->
                presets.addView(Button(this@MainActivity).apply {
                    text = "$megabytes MB"
                    contentDescription = "Ustaw limit pobierania $megabytes megabajtów"
                    setOnClickListener {
                        dataAmountDraft = megabytes.toString()
                        dataUnitDraft = "MB"
                        renderScenario(TestType.DATA)
                    }
                })
            }
            addView(optionalFields("Szybki wybór ilości danych") { addView(presets) })
            addView(createBodyText(getString(R.string.data_amount_hint)))
            addView(spaceVertical(dimen(6)))
            addView(amount)
            val units = com.example.testdialer.data.DataVolume.units
            val unit = android.widget.Spinner(this@MainActivity).apply {
                adapter = android.widget.ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, units)
                setSelection(units.indexOf(dataUnitDraft).coerceAtLeast(0))
                minimumHeight = dimen(48)
                contentDescription = getString(R.string.data_unit)
                onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) { dataUnitDraft = units[position] }
                    override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
                }
            }
            addView(unit)
            addView(createStatusText(getString(R.string.data_units_hint)))
            addView(spaceVertical(dimen(8)))
            addView(createBodyText(getString(R.string.data_url_hint)))
            addView(url)
            addView(spaceVertical(dimen(8)))
            addView(optionalFields(getString(R.string.execution_optional_details)) {
                addView(spaceVertical(dimen(8)))
                addView(label)
            })
            state.error?.let {
                addView(spaceVertical(dimen(10)))
                addView(createStatusText(it).apply { setTextColor(ColorPalette.bad) })
            }
            addView(spaceVertical(dimen(14)))
            addView(Button(this@MainActivity).apply {
                setText(R.string.data_start)
                isAllCaps = false
                textSize = 17f
                minHeight = dimen(52)
                isEnabled = !state.busy
                background = pillBackground(ColorPalette.accent)
                setTextColor(ColorPalette.onAccent)
                setOnClickListener {
                    val target = runCatching { com.example.testdialer.data.DataVolume.parse(amount.text.toString(), unit.selectedItem.toString()) }
                        .getOrElse { amount.error = it.message; return@setOnClickListener }
                    activeRunState.active?.let {
                        runCatching { activeRunViewModel.beginExecution(selectedActiveTaskId, ServiceType.DATA) }
                            .getOrElse { error ->
                                Toast.makeText(this@MainActivity, error.message, Toast.LENGTH_LONG).show()
                                return@setOnClickListener
                            }
                    }
                    cellularDataViewModel.start(
                        CellularDataInput(
                            url = url.text.toString(),
                            label = label.text.toString().trim().takeIf(String::isNotEmpty),
                            targetBytes = target,
                        ),
                    )
                }
            })

        }
    }

    private fun createGuidedSmsScenario(): View {
        val state = guidedSmsState
        if (state.awaitingObservation) return createSmsObservationPanel()
        if (state.saved) return createSmsSavedPanel()
        return createCard {
            addView(createCardTitle(getString(R.string.sms_card_title)))
            addView(spaceVertical(dimen(8)))
            addView(createBodyText(getString(R.string.sms_card_description)))
            addView(spaceVertical(dimen(14)))
            val taskAction = selectedTaskAction() as? TestAction.Sms
            val initialDestination = smsDestinationDraft ?: taskAction?.destination.orEmpty()
            val initialMessage = smsMessageDraft ?: taskAction?.message.orEmpty()
            val labelInput = createOptionalInput(getString(R.string.sms_label_hint)).apply {
                setText(smsLabelDraft.orEmpty())
                trackDraft { smsLabelDraft = it }
            }
            val destinationInput = createPhoneInput(getString(R.string.sms_destination_hint)).apply {
                setText(initialDestination)
                trackDraft { smsDestinationDraft = it }
            }
            val messageInput = createOptionalInput(getString(R.string.sms_message_hint)).apply {
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                minLines = 2
                gravity = Gravity.TOP
                setText(initialMessage)
                trackDraft { smsMessageDraft = it }
            }
            addView(createBodyText(getString(R.string.sms_destination_hint)))
            addView(spaceVertical(dimen(6)))
            addView(destinationInput)
            addView(spaceVertical(dimen(10)))
            addView(createBodyText(getString(R.string.sms_message_hint)))
            addView(spaceVertical(dimen(6)))
            addView(messageInput)
            addView(spaceVertical(dimen(8)))
            addView(optionalFields(getString(R.string.execution_optional_details)) {
                addView(spaceVertical(dimen(8)))
                addView(labelInput)
            })
            state.error?.let {
                addView(spaceVertical(dimen(10)))
                addView(createStatusText(it).apply { setTextColor(ColorPalette.bad) })
            }
            addView(spaceVertical(dimen(14)))
            addView(Button(this@MainActivity).apply {
                setText(R.string.sms_open_composer)
                isAllCaps = false
                textSize = 17f
                minHeight = dimen(52)
                isEnabled = !state.busy
                background = pillBackground(ColorPalette.accent)
                setTextColor(ColorPalette.onAccent)
                setOnClickListener {
                    val destination = destinationInput.text.toString().trim()
                    val message = messageInput.text.toString()
                    val intent = runCatching { SmsComposerIntentFactory.create(destination, message) }
                        .getOrElse {
                            Toast.makeText(this@MainActivity, R.string.sms_required, Toast.LENGTH_LONG).show()
                            return@setOnClickListener
                        }
                    if (intent.resolveActivity(packageManager) == null) {
                        Toast.makeText(this@MainActivity, R.string.sms_composer_unavailable, Toast.LENGTH_LONG).show()
                        return@setOnClickListener
                    }
                    activeRunState.active?.let {
                        runCatching { activeRunViewModel.beginExecution(selectedActiveTaskId, ServiceType.SMS) }
                            .getOrElse { error ->
                                Toast.makeText(this@MainActivity, error.message, Toast.LENGTH_LONG).show()
                                return@setOnClickListener
                            }
                    }
                    guidedSmsViewModel.start(
                        GuidedSmsInput(
                            destination = destination,
                            message = message,
                            label = labelInput.text.toString().trim().takeIf { it.isNotEmpty() },
                        ),
                    )
                }
            })
            if (state.busy) {
                addView(spaceVertical(dimen(10)))
                addView(createStatusText(getString(R.string.sms_starting)))
            }
        }
    }

    private fun openSmsComposer(state: GuidedSmsUiState) {
        val input = state.input ?: return
        val intent = SmsComposerIntentFactory.create(input.destination, input.message)
        guidedSmsViewModel.composerOpened()
        try {
            startActivity(intent)
        } catch (_: android.content.ActivityNotFoundException) {
            guidedSmsViewModel.composerLaunchFailed()
            Toast.makeText(this, R.string.sms_composer_unavailable_after_start, Toast.LENGTH_LONG).show()
        }
    }

    private fun createSmsObservationPanel(): View = createCard {
        announceForAccessibility(getString(R.string.sms_returned_announcement))
        addView(createCardTitle(getString(R.string.sms_observation_title)))
        addView(spaceVertical(dimen(8)))
        addView(createBodyText(getString(R.string.sms_observation_description)))
        addView(spaceVertical(dimen(16)))
        addView(createSmsOutcomeButton(R.string.sms_user_reported_sent, GuidedSmsOutcome.USER_REPORTED_SENT, ColorPalette.ok))
        addView(spaceVertical(dimen(10)))
        addView(createSmsOutcomeButton(R.string.sms_user_reported_not_sent, GuidedSmsOutcome.USER_REPORTED_NOT_SENT, ColorPalette.bad))
        addView(spaceVertical(dimen(10)))
        addView(createSmsOutcomeButton(R.string.sms_not_verified, GuidedSmsOutcome.NOT_VERIFIED, ColorPalette.neutral))
        if (guidedSmsState.busy) {
            addView(spaceVertical(dimen(10)))
            addView(createStatusText(getString(R.string.manual_session_saving)))
        }
    }

    private fun createSmsOutcomeButton(label: Int, outcome: GuidedSmsOutcome, color: Int): Button =
        Button(this).apply {
            setText(label)
            isAllCaps = false
            textSize = 17f
            minHeight = dimen(52)
            isEnabled = !guidedSmsState.busy
            background = pillBackground(color)
            setTextColor(ColorPalette.onAccent)
            contentDescription = getString(R.string.sms_outcome_accessibility, getString(label))
            setOnClickListener { guidedSmsViewModel.record(outcome) }
        }

    private fun createSmsSavedPanel(): View = createCard {
        addView(createCardTitle(getString(R.string.sms_saved_title)))
        addView(spaceVertical(dimen(8)))
        addView(createBodyText(getString(R.string.sms_saved_description)))
        addView(spaceVertical(dimen(14)))
        addView(Button(this@MainActivity).apply {
            setText(R.string.go_to_register)
            isAllCaps = false
            minHeight = dimen(52)
            setOnClickListener { showSection(AppSection.REGISTER) }
        })
        addView(spaceVertical(dimen(10)))
        addView(Button(this@MainActivity).apply {
            setText(R.string.sms_start_another)
            isAllCaps = false
            minHeight = dimen(52)
            setOnClickListener { guidedSmsViewModel.startAnother() }
        })
    }

    private fun createVoiceScenario(): View {
        if (awaitingVoiceOutcome) return createVoiceOutcomePanel()
        if (resultSaved) return createVoiceSavedPanel()
        return createCard {
            addView(createCardTitle(getString(R.string.voice_card_title)))
            addView(spaceVertical(dimen(8)))
            addView(createBodyText(getString(R.string.voice_card_description)))
            addView(spaceVertical(dimen(14)))
            voiceNameInput = createOptionalInput(getString(R.string.voice_name_hint))
            voiceNameInput.setText(voiceNameDraft.orEmpty())
            voiceNameInput.trackDraft { voiceNameDraft = it }
            voicePhoneInput = createPhoneInput(getString(R.string.voice_number_hint))
            voicePhoneInput.setText(voicePhoneDraft ?: (selectedTaskAction() as? TestAction.Voice)?.destination.orEmpty())
            voicePhoneInput.trackDraft { voicePhoneDraft = it }
            addView(createBodyText(getString(R.string.voice_number_hint)))
            addView(spaceVertical(dimen(6)))
            addView(voicePhoneInput)
            addView(spaceVertical(dimen(8)))
            addView(optionalFields(getString(R.string.execution_optional_details)) {
                addView(spaceVertical(dimen(8)))
                addView(voiceNameInput)
            })
            addView(spaceVertical(dimen(14)))
            addView(createPrimaryActionButton())
            addView(spaceVertical(dimen(10)))
            voiceStatusText = createStatusText(getString(R.string.enter_phone_number))
            addView(voiceStatusText)
            voicePhoneInput.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                override fun afterTextChanged(s: Editable?) {
                    if (s.isNullOrBlank()) {
                        voiceStatusText.setText(R.string.enter_phone_number)
                    }
                }
            })
        }
    }

    private fun createVoiceOutcomePanel(): View {
        val phoneNumber = pendingPhoneNumber.orEmpty()
        return createCard {
            announceForAccessibility(getString(R.string.voice_outcome_accessibility_announcement))
            addView(createCardTitle(getString(R.string.voice_outcome_title)))
            addView(spaceVertical(dimen(8)))
            addView(createBodyText(getString(R.string.voice_outcome_description, phoneNumber)))
            addView(spaceVertical(dimen(18)))
            addView(createOutcomeButton(R.string.outcome_success, VoiceTestResult.Outcome.SUCCESS, ColorPalette.ok))
            addView(spaceVertical(dimen(12)))
            addView(createOutcomeButton(R.string.outcome_failure, VoiceTestResult.Outcome.FAILURE, ColorPalette.bad))
            addView(spaceVertical(dimen(12)))
            addView(createOutcomeButton(R.string.outcome_not_checked, VoiceTestResult.Outcome.NOT_CHECKED, ColorPalette.neutral))
        }
    }

    private fun createOutcomeButton(labelRes: Int, outcome: VoiceTestResult.Outcome, color: Int): Button {
        return Button(this@MainActivity).apply {
            setText(labelRes)
            isAllCaps = false
            textSize = 18f
            minHeight = dimen(58)
            background = pillBackground(color)
            setTextColor(ColorPalette.onAccent)
            contentDescription = getString(R.string.outcome_button_description, getString(labelRes))
            setOnClickListener { saveVoiceOutcome(outcome) }
        }
    }

    private fun saveVoiceOutcome(outcome: VoiceTestResult.Outcome) {
        val phoneNumber = pendingPhoneNumber ?: return
        voiceResultStore.save(
            VoiceTestResult(
                id = UUID.randomUUID().toString(),
                outcome = outcome,
                timestampMillis = System.currentTimeMillis(),
                phoneNumber = phoneNumber,
                testName = pendingTestName,
            ),
        )
        activeRunViewModel.recordVoice(phoneNumber, outcome)
        awaitingVoiceOutcome = false
        resultSaved = true
        announceResult("voice:${System.currentTimeMillis()}", "Wynik testu połączenia zapisany: ${when (outcome) { VoiceTestResult.Outcome.SUCCESS -> "udało się"; VoiceTestResult.Outcome.FAILURE -> "nie udało się"; VoiceTestResult.Outcome.NOT_CHECKED -> "nie sprawdzono" }}")
        renderRegister()
        Toast.makeText(this@MainActivity, R.string.voice_result_saved, Toast.LENGTH_LONG).show()
        renderScenario(TestType.VOICE)
    }

    private fun createVoiceSavedPanel(): View {
        return createCard {
            addView(createCardTitle(getString(R.string.voice_result_saved_title)))
            addView(spaceVertical(dimen(8)))
            addView(createBodyText(getString(R.string.voice_result_saved_description)))
            addView(spaceVertical(dimen(16)))
            addView(Button(this@MainActivity).apply {
                setText(R.string.go_to_register)
                isAllCaps = false
                textSize = 17f
                minHeight = dimen(52)
                background = pillBackground(ColorPalette.accent)
                setTextColor(ColorPalette.onAccent)
                setOnClickListener { showSection(AppSection.REGISTER) }
            })
            addView(spaceVertical(dimen(12)))
            addView(Button(this@MainActivity).apply {
                setText(R.string.start_another_voice_test)
                isAllCaps = false
                textSize = 16f
                minHeight = dimen(50)
                background = pillBackground(ColorPalette.button)
                setTextColor(ColorPalette.textPrimary)
                setOnClickListener {
                    resultSaved = false
                    pendingPhoneNumber = null
                    pendingTestName = null
                    renderScenario(TestType.VOICE)
                }
            })
        }
    }

    private fun createPlaceholderScenario(title: String, body: String, tag: String): View {
        return createCard {
            addView(createCardTitle(title))
            addView(spaceVertical(dimen(8)))
            addView(createBodyText(body))
            addView(spaceVertical(dimen(14)))
            addView(createTag(tag))
        }
    }

    private fun createPrimaryActionButton(): View {
        return Button(this@MainActivity).apply {
            text = getString(R.string.dial_test)
            isAllCaps = false
            textSize = 16f
            minHeight = dimen(48)
            background = pillBackground(ColorPalette.accent)
            setTextColor(ColorPalette.onAccent)
            setOnClickListener {
                val number = voicePhoneInput.text.toString().trim()
                if (number.isEmpty()) {
                    voiceStatusText.setText(R.string.enter_phone_number)
                    Toast.makeText(this@MainActivity, R.string.enter_phone_number, Toast.LENGTH_SHORT).show()
                } else {
                    voiceStatusText.setText(R.string.opening_dialer)
                    pendingPhoneNumber = number
                    pendingTestName = voiceNameInput.text.toString().trim().takeIf(String::isNotEmpty)
                    resultSaved = false
                    activeRunState.active?.let {
                        runCatching { activeRunViewModel.beginExecution(selectedActiveTaskId, ServiceType.VOICE) }
                            .getOrElse { error ->
                                Toast.makeText(this@MainActivity, error.message, Toast.LENGTH_LONG).show()
                                return@setOnClickListener
                            }
                    }
                    try {
                        dialerWasOpened = true
                        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(number))))
                    } catch (_: android.content.ActivityNotFoundException) {
                        activeRunViewModel.cancelExecution(ServiceType.VOICE)
                        dialerWasOpened = false
                        pendingPhoneNumber = null
                        pendingTestName = null
                        voiceStatusText.setText(R.string.dialer_unavailable)
                        Toast.makeText(this@MainActivity, R.string.dialer_unavailable, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun createOptionalInput(hint: String): EditText {
        return EditText(this).apply {
            this.hint = hint
            inputType = InputType.TYPE_CLASS_TEXT
            textSize = 16f
            setPadding(dimen(14), dimen(12), dimen(14), dimen(12))
            background = fieldBackground()
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
    }

    private fun createPhoneInput(hint: String): EditText {
        return EditText(this).apply {
            this.hint = hint
            inputType = InputType.TYPE_CLASS_PHONE
            textSize = 18f
            setPadding(dimen(14), dimen(12), dimen(14), dimen(12))
            background = fieldBackground()
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
    }

    private fun createSystemStatusStrip(): SystemStatusStripView = SystemStatusStripView(
        context = this,
        simLabel = getString(R.string.status_sim_label),
        networkLabel = getString(R.string.status_network_label),
        cellularLabel = getString(R.string.status_cellular_label),
        simSymbol = getString(R.string.status_sim_symbol),
        networkSymbol = getString(R.string.status_network_symbol),
        cellularSymbol = getString(R.string.status_cellular_symbol),
        wifiLabel = getString(R.string.status_wifi_label),
        wifiSymbol = getString(R.string.status_wifi_symbol),
    ).also {
        it.render(isSimReady(), isCellularConnected(), isMobileDataEnabled(), isWifiConnected())
    }

    private fun refreshVoiceStatusBar() {
        if (!::systemStatusStrip.isInitialized) return
        systemStatusStrip.render(isSimReady(), isCellularConnected(), isMobileDataEnabled(), isWifiConnected())
    }

    private fun isWifiConnected(): Boolean {
        return runCatching {
            connectivityManager.allNetworks.any { network ->
                connectivityManager.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            }
        }.getOrDefault(false)
    }

    private fun isCellularConnected(): Boolean {
        return runCatching {
            connectivityManager.allNetworks.any { network ->
                connectivityManager.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
            }
        }.getOrDefault(false)
    }

    private fun isSimReady(): Boolean {
        return runCatching {
            telephonyManager.simState == TelephonyManager.SIM_STATE_READY && telephonyManager.hasIccCard()
        }.getOrDefault(false)
    }

    private fun isMobileDataEnabled(): Boolean {
        return runCatching { telephonyManager.isDataEnabled }.getOrDefault(false)
    }

    private fun createSectionHeader(title: String, description: String): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(createHeaderText(title).apply { ViewCompat.setAccessibilityHeading(this, true) })
            addView(spaceVertical(dimen(6)))
            addView(createBodyText(description))
        }
    }

    private fun createCardTitle(text: String): TextView {
        return TextView(this@MainActivity).apply {
            this.text = text
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(ColorPalette.textPrimary)
        }
    }

    private fun createHeaderText(text: String): TextView {
        return TextView(this@MainActivity).apply {
            this.text = text
            textSize = 27f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(ColorPalette.textPrimary)
        }
    }

    private fun createBodyText(text: String): TextView {
        return TextView(this@MainActivity).apply {
            this.text = text
            textSize = 16f
            setTextColor(ColorPalette.textSecondary)
        }
    }

    private fun createTinyLabel(text: String): TextView {
        return TextView(this@MainActivity).apply {
            this.text = text.uppercase()
            textSize = 12f
            setTextColor(ColorPalette.textSecondary)
        }
    }

    private fun createMicroText(text: String): TextView {
        return TextView(this@MainActivity).apply {
            this.text = text
            textSize = 13f
            setTextColor(ColorPalette.textPrimary)
        }
    }

    private fun createStatusText(text: String): TextView {
        return TextView(this@MainActivity).apply {
            this.text = text
            textSize = 14f
            setTextColor(ColorPalette.textSecondary)
        }
    }

    private fun createTag(text: String): TextView {
        return TextView(this@MainActivity).apply {
            this.text = text
            textSize = 12f
            setTextColor(ColorPalette.textPrimary)
            setPadding(dimen(10), dimen(6), dimen(10), dimen(6))
            background = pillBackground(ColorPalette.button)
        }
    }

    private fun createCard(builder: LinearLayout.() -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dimen(18), dimen(18), dimen(18), dimen(18))
            background = cardBackground()
            elevation = dimen(1).toFloat()
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            builder()
        }
    }

    private fun spaceVertical(height: Int): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                height,
            )
        }
    }

    private fun spaceHorizontal(width: Int): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(width, 1)
        }
    }

    private fun cardBackground(): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = dimen(18).toFloat()
            setColor(ColorPalette.surface)
            setStroke(dimen(1), ColorPalette.border)
        }
    }

    private fun fieldBackground(): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = dimen(14).toFloat()
            setColor(ColorPalette.surface)
            setStroke(dimen(1), ColorPalette.border)
        }
    }

    private fun pillBackground(color: Int): android.graphics.drawable.Drawable {
        val shape = GradientDrawable().apply {
            cornerRadius = dimen(12).toFloat()
            setColor(color)
        }
        return android.graphics.drawable.RippleDrawable(ColorStateList.valueOf(0x33000000), shape, null)
    }

    private fun dimen(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    private companion object {
        const val STATE_PENDING_PHONE = "pendingPhone"
        const val STATE_PENDING_NAME = "pendingName"
        const val STATE_DIALER_OPENED = "dialerOpened"
        const val STATE_AWAITING_OUTCOME = "awaitingOutcome"
        const val STATE_RESULT_SAVED = "resultSaved"
        const val STATE_CURRENT_SECTION = "currentSection"
        const val STATE_CURRENT_TEST_TYPE = "currentTestType"
        const val STATE_ACTIVE_TASK_ID = "activeTaskId"
        const val STATE_VOICE_NAME_DRAFT = "voiceNameDraft"
        const val STATE_VOICE_PHONE_DRAFT = "voicePhoneDraft"
        const val STATE_SMS_LABEL_DRAFT = "smsLabelDraft"
        const val STATE_SMS_DESTINATION_DRAFT = "smsDestinationDraft"
        const val STATE_SMS_MESSAGE_DRAFT = "smsMessageDraft"
        const val STATE_DATA_LABEL_DRAFT = "dataLabelDraft"
        const val STATE_DATA_URL_DRAFT = "dataUrlDraft"
    }

    private val ColorPalette get() = com.example.testdialer.ui.UiPalette(this)
}
