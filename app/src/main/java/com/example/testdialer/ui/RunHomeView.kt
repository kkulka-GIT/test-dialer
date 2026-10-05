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
    private val planningHost = verticalHost()
    val executionNavigationHost = verticalHost()
    val statusHost = verticalHost()
    val runHost = verticalHost()
    val taskListHost = verticalHost()
    val selectorHost = verticalHost()
    val executionContextHost = verticalHost()
    val scenarioHost = verticalHost()
    val manualSessionHost = verticalHost()
    val tasksHeading = cardTitle(tasksTitle).apply {
        ViewCompat.setAccessibilityHeading(this, true)
        isFocusable = true
        isFocusableInTouchMode = true
    }

    init {
        orientation = VERTICAL
        addView(TextView(context).apply {
            text = "TEST DIALER"
            textSize = 12f
            letterSpacing = 0.14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(palette.accent)
        })
        addView(space(6))
        addView(header(title))
        addView(space(4))
        addView(body(description))
        addView(space(10))
        addView(statusHost)
        addView(space(10))
        addView(runHost)
        addView(space(14))
        planningHost.addView(tasksHeading)
        planningHost.addView(space(6))
        planningHost.addView(body(tasksDescription))
        planningHost.addView(space(12))
        planningHost.addView(taskListHost)
        planningHost.addView(space(8))
        planningHost.addView(Button(context).apply {
            text = addTestLabel
            isAllCaps = false
            textSize = 16f
            minHeight = dp(48)
            contentDescription = addTestLabel
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(palette.button)
                setStroke(dp(1), palette.border)
            }
            setTextColor(palette.textPrimary)
            setOnClickListener { onAddTest() }
        })
        planningHost.addView(space(8))
        planningHost.addView(selectorHost)
        planningHost.addView(space(8))
        addView(planningHost)
        addView(executionNavigationHost)
        addView(executionContextHost)
        addView(space(8))
        addView(scenarioHost)
        addView(space(12))
        addView(manualSessionHost)
    }

    fun showExecutionOnly(focused: Boolean) {
        runHost.visibility = if (focused) View.GONE else View.VISIBLE
        planningHost.visibility = if (focused) View.GONE else View.VISIBLE
        executionNavigationHost.visibility = if (focused) View.VISIBLE else View.GONE
    }

    fun announceTasks(message: String) {
        tasksHeading.requestFocus()
        tasksHeading.announceForAccessibility(message)
    }

    private fun verticalHost() = LinearLayout(context).apply { orientation = VERTICAL }

    private fun card() = LinearLayout(context).apply {
        orientation = VERTICAL
        setPadding(dp(14), dp(14), dp(14), dp(14))
        background = GradientDrawable().apply {
            cornerRadius = dp(14).toFloat()
            setColor(palette.surface)
            setStroke(dp(1), palette.border)
        }
        elevation = dp(1).toFloat()
        layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun header(value: String) = TextView(context).apply {
        text = value
        textSize = 27f
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        setTextColor(palette.textPrimary)
        ViewCompat.setAccessibilityHeading(this, true)
    }

    private fun cardTitle(value: String) = TextView(context).apply {
        text = value
        textSize = 18f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(palette.textPrimary)
    }

    private fun body(value: String) = TextView(context).apply {
        text = value
        textSize = 16f
        setTextColor(palette.textSecondary)
    }

    private fun space(height: Int) = View(context).apply {
        layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(height))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private val palette get() = UiPalette(context)
}
