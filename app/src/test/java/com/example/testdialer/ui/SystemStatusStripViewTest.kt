package com.example.testdialer.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SystemStatusStripViewTest {
    @Test
    fun `strip exposes truthful status descriptions and touch sized badges`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val strip = SystemStatusStripView(context, "Wi-Fi", "Dane", "SIM", "W", "D", "S")

        strip.render(wifiAvailable = true, cellularAvailable = false, simReady = true)

        assertTrue(strip.contentDescription.contains("Wi-Fi: dostępne"))
        assertTrue(strip.contentDescription.contains("Dane: niedostępne"))
        assertTrue(strip.contentDescription.contains("SIM: dostępne"))
        repeat(strip.childCount) { index ->
            val child = strip.getChildAt(index)
            if (child is android.widget.LinearLayout) {
                assertTrue(child.minimumHeight >= (48 * context.resources.displayMetrics.density).toInt())
            }
        }
    }
}
