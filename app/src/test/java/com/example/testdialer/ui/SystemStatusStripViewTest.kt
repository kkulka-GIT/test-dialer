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
    fun `strip exposes four truthful statuses in a compact container`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val strip = SystemStatusStripView(
            context, "SIM", "Sieć", "Dane", "S", "N", "D", "Wi-Fi", "W",
        )

        strip.render(
            simReady = true,
            networkAvailable = true,
            cellularDataEnabled = false,
            wifiAvailable = true,
        )

        assertTrue(strip.contentDescription.contains("SIM: dostępne"))
        assertTrue(strip.contentDescription.contains("Sieć: dostępne"))
        assertTrue(strip.contentDescription.contains("Dane: niedostępne"))
        assertTrue(strip.contentDescription.contains("Wi-Fi: dostępne"))
        var badgeCount = 0
        repeat(strip.childCount) { index ->
            val child = strip.getChildAt(index)
            if (child is android.widget.LinearLayout) {
                badgeCount += 1
                assertTrue(child.minimumHeight <= (40 * context.resources.displayMetrics.density).toInt())
            }
        }
        assertTrue(badgeCount == 4)
    }
}
