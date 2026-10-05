package com.example.testdialer.templates

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.testdialer.domain.TestAction
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TestTemplateStoreTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    @Before fun clear() { context.getSharedPreferences("test-templates-v1", Context.MODE_PRIVATE).edit().clear().commit() }
    @Test fun `all service parameters persist across reopening and deletion is selective`() {
        val store = TestTemplateStore(context)
        val sms = TestAction.Sms("+48123", "Zażółć\nTest")
        assertTrue(store.save(" SMS ", sms))
        assertTrue(store.save("Data", TestAction.Data("https://example.com/file"), 500_000_000))
        assertTrue(store.save("Voice", TestAction.Voice("456")))
        val reopened = TestTemplateStore(context)
        assertEquals(sms, reopened.list()[0].action)
        assertEquals("SMS", reopened.list()[0].name)
        assertEquals(500_000_000L, reopened.list()[1].requestedBytes)
        assertTrue(reopened.delete(reopened.list()[1].id))
        assertEquals(listOf("SMS", "Voice"), store.list().map { it.name })
    }
    @Test fun `invalid data amount and blank label do not mutate templates`() {
        val store = TestTemplateStore(context)
        assertThrows(IllegalArgumentException::class.java) { store.save(" ", TestAction.Voice("123")) }
        assertThrows(IllegalArgumentException::class.java) { store.save("Invalid", TestAction.Data("https://example.com"), 0) }
        assertThrows(IllegalArgumentException::class.java) { store.save("Invalid", TestAction.Voice("123"), 100) }
        assertTrue(store.list().isEmpty())
    }
    @Test fun `malformed entry does not hide valid neighboring templates`() {
        val store = TestTemplateStore(context)
        store.save("Valid", TestAction.Voice("123"))
        val prefs = context.getSharedPreferences("test-templates-v1", Context.MODE_PRIVATE)
        val data = org.json.JSONArray(prefs.getString("items", "[]"))
        data.put(org.json.JSONObject().put("type", "UNKNOWN"))
        prefs.edit().putString("items", data.toString()).commit()
        assertEquals(listOf("Valid"), store.list().map { it.name })
    }
}
