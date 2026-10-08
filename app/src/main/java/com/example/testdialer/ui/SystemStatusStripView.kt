package com.example.testdialer.ui

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

internal class SystemStatusStripView(context: Context) : LinearLayout(context) {
    private val rows = List(5) { TextView(context).apply {
        textSize = 16f
        setTextColor(UiPalette(context).textPrimary)
        setPadding(dp(12), dp(6), dp(12), dp(6))
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    } }
    private var lastStatus: PhoneNetworkStatus? = null
    init {
        orientation = VERTICAL
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        rows.forEach { addView(it) }
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    fun render(status: PhoneNetworkStatus) {
        if (status == lastStatus) return
        status.rows.forEachIndexed { index, text -> rows[index].text = text }
        contentDescription = status.rows.joinToString(". ")
        lastStatus = status
    }
}
