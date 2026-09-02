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
    simLabel: String,
    networkLabel: String,
    cellularLabel: String,
    simSymbol: String,
    networkSymbol: String,
    cellularSymbol: String,
    wifiLabel: String,
    wifiSymbol: String,
) : LinearLayout(context) {
    private val simBadge = createBadge(simSymbol, simLabel)
    private val networkBadge = createBadge(networkSymbol, networkLabel)
    private val cellularBadge = createBadge(cellularSymbol, cellularLabel)
    private val wifiBadge = createBadge(wifiSymbol, wifiLabel)

    init {
        orientation = HORIZONTAL
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        background = GradientDrawable().apply {
            cornerRadius = dp(12).toFloat()
            setColor(STRIP_BACKGROUND)
            setStroke(dp(1), BORDER)
        }
        setPadding(dp(4), dp(4), dp(4), dp(4))
        addView(simBadge)
        addSpacer()
        addView(networkBadge)
        addSpacer()
        addView(cellularBadge)
        addSpacer()
        addView(wifiBadge)
    }

    fun render(simReady: Boolean, networkAvailable: Boolean, cellularDataEnabled: Boolean, wifiAvailable: Boolean) {
        updateBadge(simBadge, simReady)
        updateBadge(networkBadge, networkAvailable)
        updateBadge(cellularBadge, cellularDataEnabled)
        updateBadge(wifiBadge, wifiAvailable)
        contentDescription = listOf(
            badgeDescription(simBadge, simReady),
            badgeDescription(networkBadge, networkAvailable),
            badgeDescription(cellularBadge, cellularDataEnabled),
            badgeDescription(wifiBadge, wifiAvailable),
        ).joinToString(". ")
    }

    private fun createBadge(symbol: String, label: String): LinearLayout = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER
        minimumHeight = dp(36)
        setPadding(dp(6), dp(4), dp(6), dp(4))
        layoutParams = LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        tag = label
        addView(TextView(context).apply {
            text = symbol
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(TEXT)
        })
        addView(TextView(context).apply {
            text = " $label"
            textSize = 11f
            setTextColor(TEXT)
        })
    }

    private fun updateBadge(badge: LinearLayout, available: Boolean) {
        badge.background = GradientDrawable().apply {
            cornerRadius = dp(9).toFloat()
            setColor(if (available) AVAILABLE_BACKGROUND else UNAVAILABLE_BACKGROUND)
        }
        repeat(badge.childCount) { index ->
            (badge.getChildAt(index) as? TextView)?.setTextColor(if (available) AVAILABLE else UNAVAILABLE)
        }
        badge.contentDescription = badgeDescription(badge, available)
    }

    private fun badgeDescription(badge: LinearLayout, available: Boolean): String =
        "${badge.tag}: ${if (available) "dostępne" else "niedostępne"}"

    private fun addSpacer() {
        addView(View(context).apply { layoutParams = LayoutParams(dp(2), 1) })
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val AVAILABLE = 0xFF236B3A.toInt()
        const val UNAVAILABLE = 0xFF66788A.toInt()
        const val AVAILABLE_BACKGROUND = 0xFFE8F5EC.toInt()
        const val UNAVAILABLE_BACKGROUND = 0xFFF0F3F6.toInt()
        const val STRIP_BACKGROUND = 0xFFF8FAFC.toInt()
        const val BORDER = 0xFFD7E1EE.toInt()
        const val TEXT = 0xFF102A43.toInt()
    }
}
