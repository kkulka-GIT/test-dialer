package com.example.testdialer.ui

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

internal class SystemStatusStripView(context: Context) : LinearLayout(context) {
    private val rows = List(5) { TextView(context).apply {
        textSize = 16f
        setTextColor(UiPalette(context).textPrimary)
        setPadding(12, 6, 12, 6)
    } }
    private var lastStatus: PhoneNetworkStatus? = null
    init {
        orientation = VERTICAL
        accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        rows.forEach { addView(it) }
    }
    fun render(status: PhoneNetworkStatus) {
        if (status == lastStatus) return
        status.rows.forEachIndexed { index, text -> rows[index].text = text }
        lastStatus = status
    }
}
