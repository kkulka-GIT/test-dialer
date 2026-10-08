package com.example.testdialer.ui

import android.content.Context
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SystemStatusStripViewTest {
    @Test fun `unknown states stay distinct from disabled states and VPN is textual`() {
        val strip = SystemStatusStripView(ApplicationProvider.getApplicationContext<Context>())
        strip.render(PhoneNetworkStatus("nie ustalono", "dane wyłączone", "włączone · niepołączone", "aktywny", "VPN"))
        assertEquals(5, strip.childCount)
        assertEquals("SIM: nie ustalono", (strip.getChildAt(0) as TextView).text.toString())
        assertEquals("VPN: aktywny", (strip.getChildAt(3) as TextView).text.toString())
        assertEquals("Sieć domyślna aplikacji: VPN", (strip.getChildAt(4) as TextView).text.toString())
        strip.render(PhoneNetworkStatus("gotowa", "dane włączone", "wyłączone", "nieaktywny", "cellular"))
        assertEquals("Wi-Fi: wyłączone", (strip.getChildAt(2) as TextView).text.toString())
    }
}
