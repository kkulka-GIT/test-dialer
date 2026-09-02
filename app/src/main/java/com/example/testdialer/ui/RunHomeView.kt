package com.example.testdialer.ui

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat

internal class RunHomeView(
    context: Context,
    title: String,
    description: String,
    emptyTitle: String,
    emptyDescription: String,
    addTestLabel: String,
    tasksTitle: String,
    tasksDescription: String,
    onAddTest: () -> Unit,
) : LinearLayout(context) {
    val statusHost = verticalHost()
    val selectorHost = verticalHost()
    val scenarioHost = verticalHost()
    val manualSessionHost = verticalHost()

    init {
        orientation = VERTICAL
        addView(card().apply {
            addView(header(title))
            addView(space(8))
            addView(body(description))
        })
        addView(space(12))
        addView(statusHost)
        addView(space(12))
        addView(card().apply {
            addView(cardTitle(emptyTitle))
            addView(space(6))
            addView(body(emptyDescription))
            addView(space(12))
            addView(Button(context).apply {
                text = addTestLabel
                isAllCaps = false
                textSize = 16f
                minHeight = dp(48)
                contentDescription = addTestLabel
                setOnClickListener { onAddTest() }
            })
        })
        addView(space(18))
        addView(cardTitle(tasksTitle).apply { ViewCompat.setAccessibilityHeading(this, true) })
        addView(space(6))
        addView(body(tasksDescription))
        addView(space(12))
        addView(selectorHost)
        addView(space(12))
        addView(scenarioHost)
        addView(space(12))
        addView(manualSessionHost)
    }

    fun announceTasks(message: String) {
        selectorHost.requestFocus()
        selectorHost.announceForAccessibility(message)
    }

    private fun verticalHost() = LinearLayout(context).apply { orientation = VERTICAL }

    private fun card() = LinearLayout(context).apply {
        orientation = VERTICAL
        setPadding(dp(18), dp(18), dp(18), dp(18))
        background = GradientDrawable().apply {
            cornerRadius = dp(18).toFloat()
            setColor(SURFACE)
            setStroke(dp(1), BORDER)
        }
        elevation = dp(4).toFloat()
        layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun header(value: String) = TextView(context).apply {
        text = value
        textSize = 27f
        setTextColor(TEXT_PRIMARY)
        ViewCompat.setAccessibilityHeading(this, true)
    }

    private fun cardTitle(value: String) = TextView(context).apply {
        text = value
        textSize = 18f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(TEXT_PRIMARY)
    }

    private fun body(value: String) = TextView(context).apply {
        text = value
        textSize = 16f
        setTextColor(TEXT_SECONDARY)
    }

    private fun space(height: Int) = View(context).apply {
        layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(height))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val SURFACE = 0xFFFFFFFF.toInt()
        const val BORDER = 0xFFD7E1EE.toInt()
        const val TEXT_PRIMARY = 0xFF102A43.toInt()
        const val TEXT_SECONDARY = 0xFF52606D.toInt()
    }
}
