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
    private val simBadge = createBadge(com.example.testdialer.R.drawable.ic_status_sim, simLabel)
    private val networkBadge = createBadge(com.example.testdialer.R.drawable.ic_status_network, networkLabel)
    private val cellularBadge = createBadge(com.example.testdialer.R.drawable.ic_status_data, cellularLabel)
    private val wifiBadge = createBadge(com.example.testdialer.R.drawable.ic_status_wifi, wifiLabel)

    init {
        orientation = HORIZONTAL
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        background = GradientDrawable().apply {
            cornerRadius = dp(12).toFloat()
            setColor(palette.surface)
            setStroke(dp(1), palette.border)
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

    private fun createBadge(icon: Int, label: String): LinearLayout = LinearLayout(context).apply {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        minimumHeight = dp(36)
        setPadding(dp(6), dp(4), dp(6), dp(4))
        layoutParams = LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        tag = label
        addView(android.widget.ImageView(context).apply {
            setImageResource(icon)
            layoutParams = LayoutParams(dp(20), dp(20))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        })
        addView(TextView(context).apply {
            text = label
            gravity = Gravity.CENTER
            textSize = 14f
            setTextColor(palette.textPrimary)
        })
    }

    private fun updateBadge(badge: LinearLayout, available: Boolean) {
        badge.background = GradientDrawable().apply {
            cornerRadius = dp(9).toFloat()
            setColor(if (available) palette.button else palette.background)
        }
        repeat(badge.childCount) { index ->
            val color = if (available) palette.ok else palette.textSecondary
            (badge.getChildAt(index) as? TextView)?.setTextColor(color)
            (badge.getChildAt(index) as? android.widget.ImageView)?.imageTintList = android.content.res.ColorStateList.valueOf(color)
        }
        badge.contentDescription = badgeDescription(badge, available)
    }

    private fun badgeDescription(badge: LinearLayout, available: Boolean): String =
        "${badge.tag}: ${if (available) "dostępne" else "niedostępne"}"

    private fun addSpacer() {
        addView(View(context).apply { layoutParams = LayoutParams(dp(2), 1) })
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private val palette get() = UiPalette(context)
}
