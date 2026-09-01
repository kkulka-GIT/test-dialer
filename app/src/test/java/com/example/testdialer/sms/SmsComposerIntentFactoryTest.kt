package com.example.testdialer.sms

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SmsComposerIntentFactoryTest {
    @Test
    fun `creates only sendto smsto intent and preserves message`() {
        val intent = SmsComposerIntentFactory.create("  +48 123#4  ", "Treść & ? test")

        assertEquals(Intent.ACTION_SENDTO, intent.action)
        assertEquals("smsto", intent.data?.scheme)
        assertEquals("+48 123#4", intent.data?.schemeSpecificPart)
        assertEquals("Treść & ? test", intent.getStringExtra("sms_body"))
        assertNull(intent.component)
        assertNull(intent.`package`)
        assertTrue(intent.categories.isNullOrEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects control characters in destination`() {
        SmsComposerIntentFactory.create("123\n456", "test")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects blank message`() {
        SmsComposerIntentFactory.create("123", "")
    }
}
