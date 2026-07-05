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
            layoutParams = ScrollView.LayoutParams(
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
            layoutParams = ScrollView.LayoutParams(
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
            layoutParams = ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }

        content.addView(createSectionHeader(
            getString(R.string.register_title),
            getString(R.string.register_description),
        ))
        content.addView(spaceVertical(dimen(16)))
        content.addView(createCard {
            addView(createCardTitle(getString(R.string.register_placeholder_title)))
            addView(spaceVertical(dimen(8)))
            addView(createBodyText(getString(R.string.register_placeholder_body)))
            addView(spaceVertical(dimen(12)))
            addView(createTag(getString(R.string.register_placeholder_tag)))
        })

        scroll.addView(content)
        return scroll
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
                    startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
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

    private object ColorPalette {
        const val background = 0xFFF4F7FB.toInt()
        const val surface = 0xFFFFFFFF.toInt()
        const val accent = 0xFF1565C0.toInt()
        const val border = 0xFFD7E1EE.toInt()
        const val textPrimary = 0xFF102A43.toInt()
        const val textSecondary = 0xFF52606D.toInt()
        const val onAccent = 0xFFFFFFFF.toInt()
        const val ok = 0xFF2E7D32.toInt()
        const val bad = 0xFFC62828.toInt()
    }
}
