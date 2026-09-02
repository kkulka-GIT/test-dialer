package com.example.testdialer.ui

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

internal class SystemStatusStripView(
    context: Context,
    wifiLabel: String,
    cellularLabel: String,
    simLabel: String,
    wifiSymbol: String,
    cellularSymbol: String,
    simSymbol: String,
) : LinearLayout(context) {
    private val wifiBadge = createBadge(wifiSymbol, wifiLabel)
    private val cellularBadge = createBadge(cellularSymbol, cellularLabel)
    private val simBadge = createBadge(simSymbol, simLabel)

    init {
        orientation = HORIZONTAL
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        addView(wifiBadge)
        addSpacer()
        addView(cellularBadge)
        addSpacer()
        addView(simBadge)
    }

    fun render(wifiAvailable: Boolean, cellularAvailable: Boolean, simReady: Boolean) {
        updateBadge(wifiBadge, wifiAvailable)
        updateBadge(cellularBadge, cellularAvailable)
        updateBadge(simBadge, simReady)
        contentDescription = listOf(
            badgeDescription(wifiBadge, wifiAvailable),
            badgeDescription(cellularBadge, cellularAvailable),
            badgeDescription(simBadge, simReady),
        ).joinToString(". ")
    }

    private fun createBadge(symbol: String, label: String): LinearLayout = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER
        minimumHeight = dp(48)
        setPadding(dp(10), dp(8), dp(10), dp(8))
        layoutParams = LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        tag = label
        addView(TextView(context).apply {
            text = symbol
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ON_STATUS)
        })
        addView(TextView(context).apply {
            text = "  $label"
            textSize = 12f
            setTextColor(ON_STATUS)
        })
    }

    private fun updateBadge(badge: LinearLayout, available: Boolean) {
        badge.background = GradientDrawable().apply {
            cornerRadius = dp(16).toFloat()
            setColor(if (available) AVAILABLE else UNAVAILABLE)
        }
        badge.contentDescription = badgeDescription(badge, available)
    }

    private fun badgeDescription(badge: LinearLayout, available: Boolean): String =
        "${badge.tag}: ${if (available) "dostępne" else "niedostępne"}"

    private fun addSpacer() {
        addView(View(context).apply { layoutParams = LayoutParams(dp(8), 1) })
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val AVAILABLE = 0xFF2E7D32.toInt()
        const val UNAVAILABLE = 0xFFC62828.toInt()
        const val ON_STATUS = 0xFFFFFFFF.toInt()
    }
}
