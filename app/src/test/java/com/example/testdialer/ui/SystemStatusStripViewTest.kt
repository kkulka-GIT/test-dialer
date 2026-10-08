package com.example.testdialer.ui

import android.content.Context
import android.content.res.Configuration
import android.widget.LinearLayout
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SystemStatusStripViewTest {
    @Test fun `compact states stay textual and one detail expands at a time`() {
        val strip = SystemStatusStripView(ApplicationProvider.getApplicationContext<Context>())
        strip.render(PhoneNetworkStatus("nie ustalono", "dane wyłączone · 4G LTE · sieć widoczna", "włączone · niepołączone", "aktywny", "VPN"))
        val firstRow = strip.getChildAt(0) as LinearLayout
        val sim = firstRow.getChildAt(0)
        val data = firstRow.getChildAt(1)
        assertTrue(sim.contentDescription.contains("SIM: nie ustalono"))
        assertTrue(data.contentDescription.contains("Dane: dane wyłączone"))
        sim.performClick()
        assertTrue(strip.isExpanded(0))
        assertEquals("SIM: nie ustalono", strip.detailText())
        data.performClick()
        assertFalse(strip.isExpanded(0))
        assertTrue(strip.isExpanded(1))
        assertTrue(strip.detailText().contains("sieć"))
    }

    @Test fun `large font uses a single column`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val configuration = Configuration(context.resources.configuration).apply { fontScale = 1.5f }
        val strip = SystemStatusStripView(context.createConfigurationContext(configuration))
        assertTrue(strip.isSingleColumn())
        assertEquals(6, strip.childCount)
    }
}
