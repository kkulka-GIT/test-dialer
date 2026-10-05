package com.example.testdialer.ui

import android.content.Context
import com.example.testdialer.R

/** Shared semantic colors, including night-mode resources. */
internal class UiPalette(private val context: Context) {
    val background get() = context.getColor(R.color.ui_background)
    val surface get() = context.getColor(R.color.ui_surface)
    val accent get() = context.getColor(R.color.ui_accent)
    val button get() = context.getColor(R.color.ui_button)
    val border get() = context.getColor(R.color.ui_border)
    val textPrimary get() = context.getColor(R.color.ui_textPrimary)
    val textSecondary get() = context.getColor(R.color.ui_textSecondary)
    val onAccent get() = context.getColor(R.color.ui_onAccent)
    val ok get() = context.getColor(R.color.ui_ok)
    val bad get() = context.getColor(R.color.ui_bad)
    val neutral get() = context.getColor(R.color.ui_neutral)
}
