package com.example.testdialer

import android.app.Activity
import android.content.Intent
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MainActivity : Activity() {
    private enum class Section {
        STATUS,
        TEST,
        REGISTER,
    }

    private enum class TestType {
        VOICE,
        SMS,
        DATA,
    }

    private val sectionButtons = mutableMapOf<Section, Button>()
    private val testTypeButtons = mutableMapOf<TestType, Button>()
    private lateinit var contentHost: FrameLayout
    private lateinit var statusSection: View
    private lateinit var testSection: View
    private lateinit var registerSection: View
    private lateinit var registerListHost: LinearLayout
    private lateinit var wifiBadge: LinearLayout
    private lateinit var cellularBadge: LinearLayout
    private lateinit var simBadge: LinearLayout
    private lateinit var testScenarioHost: LinearLayout
    private lateinit var voiceStatusText: TextView
    private lateinit var voicePhoneInput: EditText
    private lateinit var voiceNameInput: EditText
    private var networkCallbackRegistered = false
    private var currentSection = Section.STATUS
    private var currentTestType = TestType.VOICE
    private lateinit var voiceResultStore: VoiceResultStore
    private var pendingPhoneNumber: String? = null
    private var pendingTestName: String? = null
    private var dialerWasOpened = false
    private var awaitingVoiceOutcome = false
    private var resultSaved = false

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        voiceResultStore = VoiceResultStore(this)
        pendingPhoneNumber = savedInstanceState?.getString(STATE_PENDING_PHONE)
        pendingTestName = savedInstanceState?.getString(STATE_PENDING_NAME)
        dialerWasOpened = savedInstanceState?.getBoolean(STATE_DIALER_OPENED) ?: false
        awaitingVoiceOutcome = savedInstanceState?.getBoolean(STATE_AWAITING_OUTCOME) ?: false
        resultSaved = savedInstanceState?.getBoolean(STATE_RESULT_SAVED) ?: false

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

        statusSection = createStatusSection()
        testSection = createTestSection()
        registerSection = createRegisterSection()

        contentHost.addView(statusSection)
        contentHost.addView(testSection)
        contentHost.addView(registerSection)

        root.addView(contentHost)
        root.addView(createBottomNavigation())
        setContentView(root)

        showSection(Section.STATUS)
    }

    override fun onStart() {
        super.onStart()
        registerNetworkCallback()
        refreshVoiceStatusBar()
    }

    override fun onResume() {
        super.onResume()
        if (awaitingVoiceOutcome && currentTestType == TestType.VOICE && ::testScenarioHost.isInitialized) {
            renderScenario(TestType.VOICE)
        }
    }

    override fun onPause() {
        if (dialerWasOpened) {
            dialerWasOpened = false
            awaitingVoiceOutcome = true
        }
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_PENDING_PHONE, pendingPhoneNumber)
        outState.putString(STATE_PENDING_NAME, pendingTestName)
        outState.putBoolean(STATE_DIALER_OPENED, dialerWasOpened)
        outState.putBoolean(STATE_AWAITING_OUTCOME, awaitingVoiceOutcome)
        outState.putBoolean(STATE_RESULT_SAVED, resultSaved)
        super.onSaveInstanceState(outState)
    }

    override fun onStop() {
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

    private fun showSection(section: Section) {
        currentSection = section
        statusSection.visibility = if (section == Section.STATUS) View.VISIBLE else View.GONE
        testSection.visibility = if (section == Section.TEST) View.VISIBLE else View.GONE
        registerSection.visibility = if (section == Section.REGISTER) View.VISIBLE else View.GONE

        sectionButtons.forEach { (current, button) ->
            val selected = current == section
            button.isEnabled = true
            button.alpha = if (selected) 1f else 0.86f
            button.background = pillBackground(if (selected) ColorPalette.accent else ColorPalette.button)
            button.setTextColor(if (selected) ColorPalette.onAccent else ColorPalette.textPrimary)
        }

        if (section == Section.TEST) {
            refreshVoiceStatusBar()
        } else if (section == Section.REGISTER) {
            renderRegister()
        }
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

        nav.addView(createSectionButton(Section.STATUS, getString(R.string.nav_status)))
        nav.addView(spaceHorizontal(dimen(8)))
        nav.addView(createSectionButton(Section.TEST, getString(R.string.nav_test)))
        nav.addView(spaceHorizontal(dimen(8)))
        nav.addView(createSectionButton(Section.REGISTER, getString(R.string.nav_register)))
        return nav
    }

    private fun createSectionButton(section: Section, label: String): Button {
        return Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 14f
            minHeight = dimen(44)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            background = pillBackground(ColorPalette.button)
            setTextColor(ColorPalette.textPrimary)
            setOnClickListener { showSection(section) }
            sectionButtons[section] = this
        }
    }

    private fun createStatusSection(): View {
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
            getString(R.string.status_title),
            getString(R.string.status_description),
        ))
        content.addView(spaceVertical(dimen(16)))
        content.addView(createCard {
            addView(createCardTitle(getString(R.string.status_dashboard_title)))
            addView(spaceVertical(dimen(8)))
            addView(createBodyText(getString(R.string.status_dashboard_body)))
            addView(spaceVertical(dimen(16)))
            addView(createSummaryRow())
        })

        scroll.addView(content)
        return scroll
    }

    private fun createSummaryRow(): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }

        row.addView(createSummaryTile(getString(R.string.voice_type), getString(R.string.status_summary_placeholder)))
        row.addView(spaceHorizontal(dimen(8)))
        row.addView(createSummaryTile(getString(R.string.sms_type), getString(R.string.status_summary_placeholder)))
        row.addView(spaceHorizontal(dimen(8)))
        row.addView(createSummaryTile(getString(R.string.data_type), getString(R.string.status_summary_placeholder)))
        return row
    }

    private fun createSummaryTile(title: String, body: String): View {
        return createCard {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(dimen(14), dimen(14), dimen(14), dimen(14))
            addView(createTinyLabel(title))
            addView(spaceVertical(dimen(6)))
            addView(createMicroText(body))
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

        content.addView(createSectionHeader(
            getString(R.string.test_title),
            getString(R.string.test_description),
        ))
        content.addView(spaceVertical(dimen(16)))
        content.addView(createVoiceStatusBarCard())
        content.addView(spaceVertical(dimen(14)))
        content.addView(createTestTypeSelectorCard())
        content.addView(spaceVertical(dimen(14)))
        content.addView(createCard {
            testScenarioHost = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
            }
            addView(testScenarioHost)
        })

        scroll.addView(content)
        renderScenario(currentTestType)
        return scroll
    }

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
        registerListHost = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        content.addView(registerListHost)
        renderRegister()

        scroll.addView(content)
        return scroll
    }

    private fun renderRegister() {
        if (!::registerListHost.isInitialized) return
        registerListHost.removeAllViews()
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
            contentDescription = buildString {
                append(getString(R.string.result_accessibility, outcomeLabel, formattedDate, result.phoneNumber))
                result.testName?.let { append(getString(R.string.result_accessibility_name, it)) }
            }
            addView(TextView(this).apply {
                text = outcomeLabel
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(ColorPalette.onAccent)
                setPadding(dimen(12), dimen(8), dimen(12), dimen(8))
                background = pillBackground(outcomeColor)
            })
            addView(spaceVertical(dimen(12)))
            addView(createCardTitle(result.testName ?: getString(R.string.result_unnamed)))
            addView(spaceVertical(dimen(8)))
            addView(createBodyText(getString(R.string.result_type_value)))
            addView(spaceVertical(dimen(4)))
            addView(createBodyText(getString(R.string.result_phone_value, result.phoneNumber)))
            addView(spaceVertical(dimen(4)))
            addView(createBodyText(getString(R.string.result_date_value, formattedDate)))
        }
    }

    private fun createTestTypeSelectorCard(): View {
        return createCard {
            addView(createCardTitle(getString(R.string.test_selector_title)))
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
        return Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 14f
            minHeight = dimen(42)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                currentTestType = type
                updateTestTypeChips()
                renderScenario(type)
            }
            testTypeButtons[type] = this
        }
    }

    private fun updateTestTypeChips() {
        testTypeButtons.forEach { (type, button) ->
            val selected = type == currentTestType
            button.alpha = if (selected) 1f else 0.92f
            button.background = pillBackground(if (selected) ColorPalette.accent else ColorPalette.button)
            button.setTextColor(if (selected) ColorPalette.onAccent else ColorPalette.textPrimary)
        }
    }

    private fun renderScenario(type: TestType) {
        if (!::testScenarioHost.isInitialized) return
        testScenarioHost.removeAllViews()
        when (type) {
            TestType.VOICE -> testScenarioHost.addView(createVoiceScenario())
            TestType.SMS -> testScenarioHost.addView(createPlaceholderScenario(
                getString(R.string.sms_type),
                getString(R.string.sms_placeholder_body),
                getString(R.string.sms_placeholder_tag),
            ))
            TestType.DATA -> testScenarioHost.addView(createPlaceholderScenario(
                getString(R.string.data_type),
                getString(R.string.data_placeholder_body),
                getString(R.string.data_placeholder_tag),
            ))
        }
        updateTestTypeChips()
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
            addView(voiceNameInput)
            addView(spaceVertical(dimen(12)))
            voicePhoneInput = createPhoneInput(getString(R.string.voice_number_hint))
            addView(voicePhoneInput)
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
        return Button(this).apply {
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
        awaitingVoiceOutcome = false
        resultSaved = true
        renderRegister()
        Toast.makeText(this, R.string.voice_result_saved, Toast.LENGTH_LONG).show()
        renderScenario(TestType.VOICE)
    }

    private fun createVoiceSavedPanel(): View {
        return createCard {
            addView(createCardTitle(getString(R.string.voice_result_saved_title)))
            addView(spaceVertical(dimen(8)))
            addView(createBodyText(getString(R.string.voice_result_saved_description)))
            addView(spaceVertical(dimen(16)))
            addView(Button(this).apply {
                setText(R.string.go_to_register)
                isAllCaps = false
                textSize = 17f
                minHeight = dimen(52)
                background = pillBackground(ColorPalette.accent)
                setTextColor(ColorPalette.onAccent)
                setOnClickListener { showSection(Section.REGISTER) }
            })
            addView(spaceVertical(dimen(12)))
            addView(Button(this).apply {
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
        return Button(this).apply {
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
                    try {
                        dialerWasOpened = true
                        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(number))))
                    } catch (_: android.content.ActivityNotFoundException) {
                        dialerWasOpened = false
                        pendingPhoneNumber = null
                        pendingTestName = null
                        voiceStatusText.setText(R.string.dialer_unavailable)
                        Toast.makeText(this, R.string.dialer_unavailable, Toast.LENGTH_LONG).show()
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

    private fun createVoiceStatusBarCard(): View {
        return createCard {
            addView(createCardTitle(getString(R.string.voice_status_title)))
            addView(spaceVertical(dimen(12)))
            val row = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            wifiBadge = createStatusBadge(getString(R.string.status_wifi_symbol), getString(R.string.status_wifi_label))
            cellularBadge = createStatusBadge(getString(R.string.status_cellular_symbol), getString(R.string.status_cellular_label))
            simBadge = createStatusBadge(getString(R.string.status_sim_symbol), getString(R.string.status_sim_label))
            row.addView(wifiBadge)
            row.addView(spaceHorizontal(dimen(8)))
            row.addView(cellularBadge)
            row.addView(spaceHorizontal(dimen(8)))
            row.addView(simBadge)
            addView(row)
            refreshVoiceStatusBar()
        }
    }

    private fun createStatusBadge(symbol: String, label: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dimen(10), dimen(10), dimen(10), dimen(10))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            background = pillBackground(ColorPalette.bad)
            val symbolView = TextView(this@MainActivity).apply {
                text = symbol
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(ColorPalette.onAccent)
                gravity = Gravity.CENTER_HORIZONTAL
            }
            val labelView = TextView(this@MainActivity).apply {
                text = label
                textSize = 12f
                setTextColor(ColorPalette.onAccent)
                gravity = Gravity.CENTER_HORIZONTAL
            }
            addView(symbolView)
            addView(labelView)
        }
    }

    private fun refreshVoiceStatusBar() {
        if (!::wifiBadge.isInitialized || !::cellularBadge.isInitialized || !::simBadge.isInitialized) return
        setStatusBadge(wifiBadge, isWifiConnected(), ColorPalette.bad, ColorPalette.ok)
        setStatusBadge(cellularBadge, isCellularConnected(), ColorPalette.ok, ColorPalette.bad)
        setStatusBadge(simBadge, isSimReady(), ColorPalette.ok, ColorPalette.bad)
    }

    private fun setStatusBadge(view: LinearLayout, isOk: Boolean, trueColor: Int, falseColor: Int) {
        view.background = pillBackground(if (isOk) trueColor else falseColor)
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

    private fun createSectionHeader(title: String, description: String): View {
        return createCard {
            addView(createHeaderText(title))
            addView(spaceVertical(dimen(8)))
            addView(createBodyText(description))
        }
    }

    private fun createCardTitle(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 18f
            setTextColor(ColorPalette.textPrimary)
        }
    }

    private fun createHeaderText(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 27f
            setTextColor(ColorPalette.textPrimary)
        }
    }

    private fun createBodyText(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 16f
            setTextColor(ColorPalette.textSecondary)
        }
    }

    private fun createTinyLabel(text: String): TextView {
        return TextView(this).apply {
            this.text = text.uppercase()
            textSize = 12f
            setTextColor(ColorPalette.textSecondary)
        }
    }

    private fun createMicroText(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 13f
            setTextColor(ColorPalette.textPrimary)
        }
    }

    private fun createStatusText(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 14f
            setTextColor(ColorPalette.textSecondary)
        }
    }

    private fun createTag(text: String): TextView {
        return TextView(this).apply {
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
            elevation = dimen(4).toFloat()
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

    private fun pillBackground(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = dimen(20).toFloat()
            setColor(color)
        }
    }

    private fun dimen(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    private companion object {
        const val STATE_PENDING_PHONE = "pendingPhone"
        const val STATE_PENDING_NAME = "pendingName"
        const val STATE_DIALER_OPENED = "dialerOpened"
        const val STATE_AWAITING_OUTCOME = "awaitingOutcome"
        const val STATE_RESULT_SAVED = "resultSaved"
    }

    private object ColorPalette {
        const val background = 0xFFF4F7FB.toInt()
        const val surface = 0xFFFFFFFF.toInt()
        const val accent = 0xFF1565C0.toInt()
        const val button = 0xFFE8EEF5.toInt()
        const val border = 0xFFD7E1EE.toInt()
        const val textPrimary = 0xFF102A43.toInt()
        const val textSecondary = 0xFF52606D.toInt()
        const val onAccent = 0xFFFFFFFF.toInt()
        const val ok = 0xFF2E7D32.toInt()
        const val bad = 0xFFC62828.toInt()
        const val neutral = 0xFF455A64.toInt()
    }
}
