package com.example.testdialer.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class PhoneNetworkStatusReaderTest {
    @Test @Config(sdk = [26]) fun `older phones remain readable without runtime phone permission`() {
        val status = PhoneNetworkStatusReader(ApplicationProvider.getApplicationContext<Context>()).read()
        assertEquals(5, status.rows.size)
        assertTrue(status.cellular.contains("wymaga uprawnienia"))
        assertTrue(status.defaultNetwork.isNotBlank())
    }
    @Test @Config(sdk = [35]) fun `missing radio information does not invent 5G`() {
        val status = PhoneNetworkStatusReader(ApplicationProvider.getApplicationContext<Context>()).read()
        assertFalse(status.cellular.contains("5G"))
        assertTrue(status.rows.all { it.isNotBlank() })
    }
}
