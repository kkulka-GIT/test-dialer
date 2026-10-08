package com.example.testdialer.ui

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.testdialer.R

internal class SystemStatusStripView(context: Context) : LinearLayout(context) {
    private data class Item(val label: String, val icon: Int,
        val shortValue: (PhoneNetworkStatus) -> String, val detailValue: (PhoneNetworkStatus) -> String)
    private val items = listOf(
        Item("SIM", R.drawable.ic_status_sim, { it.sim.substringBefore(" (") }, { it.sim }),
        Item("Dane", R.drawable.ic_status_data, { it.cellular.substringBefore(" ·") }, { it.cellular }),
        Item("Wi-Fi", R.drawable.ic_status_wifi, { wifiShort(it.wifi) }, { it.wifi }),
        Item("VPN", R.drawable.ic_status_network, { it.vpn }, { it.vpn }),
    )
    private val buttons = mutableListOf<LinearLayout>()
    private val values = mutableListOf<TextView>()
    private val chevrons = mutableListOf<TextView>()
    private val detail = TextView(context).apply {
        textSize = 16f; setTextColor(UiPalette(context).textPrimary); setPadding(dp(12), dp(8), dp(12), dp(10)); visibility = View.GONE
    }
    private val defaultNetwork = TextView(context).apply {
        textSize = 15f; setTextColor(UiPalette(context).textSecondary); setPadding(dp(12), dp(8), dp(12), dp(6))
    }
    private var expandedIndex: Int? = null
    private var lastStatus: PhoneNetworkStatus? = null

    init {
        orientation = VERTICAL
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        buildItems()
        addView(detail)
        addView(defaultNetwork)
    }

    private fun buildItems() {
        if (resources.configuration.fontScale >= 1.3f) items.indices.forEach {
            addView(createButton(it), LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                setMargins(dp(2), dp(2), dp(2), dp(2))
            })
        }
        else items.indices.chunked(2).forEach { pair ->
            addView(LinearLayout(context).apply {
                orientation = HORIZONTAL
                pair.forEach { index ->
                    addView(createButton(index), LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                        setMargins(dp(2), dp(2), dp(2), dp(2))
                    })
                }
            })
        }
    }

    private fun createButton(index: Int) = LinearLayout(context).apply {
        orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; isClickable = true; isFocusable = true; minimumHeight = dp(48)
        setPadding(dp(10), dp(6), dp(8), dp(6))
        background = GradientDrawable().apply {
            cornerRadius = dp(12).toFloat(); setColor(UiPalette(context).surface); setStroke(dp(1), UiPalette(context).border)
        }
        addView(ImageView(context).apply {
            setImageResource(items[index].icon); importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LayoutParams(dp(24), dp(24)).apply { marginEnd = dp(8) })
        addView(LinearLayout(context).apply {
            orientation = VERTICAL
            addView(TextView(context).apply {
                text = items[index].label; textSize = 14f; typeface = Typeface.DEFAULT_BOLD
                setTextColor(UiPalette(context).textPrimary); importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            })
            addView(TextView(context).apply {
                textSize = 14f; setTextColor(UiPalette(context).textSecondary)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO; values += this
            })
        }, LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        addView(TextView(context).apply {
            text = "⌄"; textSize = 20f; setTextColor(UiPalette(context).textSecondary)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO; chevrons += this
        })
        setOnClickListener { toggle(index) }
        buttons += this
    }

    fun render(status: PhoneNetworkStatus) {
        if (status == lastStatus) return
        items.forEachIndexed { index, item -> values[index].text = item.shortValue(status) }
        defaultNetwork.text = "Sieć domyślna aplikacji: ${status.defaultNetwork}"
        lastStatus = status
        refreshExpanded(); refreshAccessibility()
    }

    private fun toggle(index: Int) {
        expandedIndex = if (expandedIndex == index) null else index
        refreshExpanded(); refreshAccessibility()
    }

    private fun refreshExpanded() {
        val index = expandedIndex
        if (index == null || lastStatus == null) detail.visibility = View.GONE else {
            detail.text = "${items[index].label}: ${items[index].detailValue(lastStatus!!)}"
            detail.visibility = View.VISIBLE
        }
        chevrons.forEachIndexed { itemIndex, view -> view.text = if (itemIndex == index) "⌃" else "⌄" }
    }

    private fun refreshAccessibility() {
        val status = lastStatus ?: return
        buttons.forEachIndexed { index, button ->
            val state = if (expandedIndex == index) "rozwinięte" else "zwinięte"
            button.contentDescription = "${items[index].label}: ${items[index].shortValue(status)}, $state"
            if (Build.VERSION.SDK_INT >= 30) button.stateDescription = state
        }
    }

    internal fun isExpanded(index: Int) = expandedIndex == index
    internal fun detailText() = detail.text.toString()
    internal fun isSingleColumn() = resources.configuration.fontScale >= 1.3f
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    companion object {
        private fun wifiShort(value: String) = when {
            value.substringBefore(" ·") == "wyłączone" -> "wyłączone"
            value.substringBefore(" ·") == "nie ustalono" -> "nie ustalono"
            "niepołączone" in value -> "niepołączone"
            "połączone" in value -> "połączone"
            else -> value.substringBefore(" ·")
        }
    }
}
