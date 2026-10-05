package com.example.testdialer.templates

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.testdialer.domain.TestAction
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TestTemplateArchiveTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val store get() = TestTemplateStore(context)
    @Before fun clear() { context.getSharedPreferences("test-templates-v1", Context.MODE_PRIVATE).edit().clear().commit() }
    private fun sample() = listOf(
        TestTemplate("same-id", "Połączenie", TestAction.Voice("+48123")),
        TestTemplate("same-id", "SMS żółć", TestAction.Sms("456", "Linia 1\n\"Zażółć\"")),
        TestTemplate("null-sms", "Brak treści", TestAction.Sms("789", null)),
        TestTemplate("data", "Dane", TestAction.Data("https://example.com/file?a=1&b=2"), 500_000_000L))

    @Test fun `portable round trip preserves parameters and import generates unique local ids`() {
        val original = sample()
        val archive = TestTemplateArchive.encode(original)
        assertFalse(archive.contains("same-id"))
        val incoming = TestTemplateArchive.read(archive.byteInputStream())
        assertEquals(4, store.previewImport(incoming).added)
        assertEquals(TemplateImportResult(4, 0), store.importItems(incoming))
        val saved = TestTemplateStore(context).list()
        assertEquals(original.map { it.copy(id = "") }, saved.map { it.copy(id = "") })
        assertEquals(4, saved.map { it.id }.toSet().size)
        assertEquals(TemplateImportResult(0, 4), store.importItems(TestTemplateArchive.decode(archive)))
        assertEquals(saved, store.list())
    }

    @Test fun `duplicates within incoming file are skipped but distinct names or parameters survive`() {
        val item = sample().first()
        val incoming = listOf(item, item.copy(id = "other"), item.copy(name = "Inny"), item.copy(action = TestAction.Voice("999")))
        assertEquals(TemplateImportResult(3, 1), store.previewImport(incoming))
        assertEquals(TemplateImportResult(3, 1), store.importItems(incoming))
    }

    @Test fun `capacity failure is atomic and preserves existing templates`() {
        repeat(49) { assertTrue(store.save("Test $it", TestAction.Voice("$it"))) }
        val before = store.list()
        assertThrows(IllegalArgumentException::class.java) { store.importItems(sample()) }
        assertEquals(before, store.list())
        assertThrows(IllegalArgumentException::class.java) { store.previewImport(sample()) }
    }

    @Test fun `unknown format version service wrong types and fractional bytes are rejected`() {
        val valid = TestTemplateArchive.encode(sample())
        val malformed = listOf(
            JSONObject(valid).put("format", "other").toString(),
            JSONObject(valid).put("schemaVersion", 2).toString(),
            JSONObject(valid).put("schemaVersion", "1").toString(),
            JSONObject(valid).apply { getJSONArray("templates").getJSONObject(0).put("type", "AUTO_CALL") }.toString(),
            JSONObject(valid).apply { getJSONArray("templates").getJSONObject(0).put("target", 123) }.toString(),
            JSONObject(valid).apply { getJSONArray("templates").getJSONObject(3).put("bytes", 1.5) }.toString(),
            JSONObject(valid).apply { getJSONArray("templates").getJSONObject(0).put("bytes", 100) }.toString(),
            JSONObject(valid).apply { getJSONArray("templates").getJSONObject(1).put("message", "x".repeat(10001)) }.toString())
        store.save("Existing", TestAction.Voice("123"))
        val before = store.list()
        malformed.forEach { text -> assertThrows(Exception::class.java) { TestTemplateArchive.decode(text) } }
        assertEquals(before, store.list())
    }

    @Test fun `oversized archive invalid utf8 and too many entries are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { TestTemplateArchive.read(ByteArray(TestTemplateArchive.MAX_FILE_BYTES + 1).inputStream()) }
        assertThrows(Exception::class.java) { TestTemplateArchive.read(byteArrayOf(0xc3.toByte(), 0x28).inputStream()) }
        assertThrows(IllegalArgumentException::class.java) { TestTemplateArchive.decode(TestTemplateArchive.encode(List(51) { sample().first() })) }
        assertTrue(TestTemplateArchive.decode(TestTemplateArchive.encode(emptyList())).isEmpty())
    }

    @Test fun `archive limit accommodates maximum escaped sms templates`() {
        val largest = List(TestTemplateStore.MAX_ITEMS) { index -> TestTemplate("$index", "N".repeat(80), TestAction.Sms("1".repeat(500), "\u0001".repeat(10000))) }
        val text = TestTemplateArchive.encode(largest)
        assertTrue(text.toByteArray().size < TestTemplateArchive.MAX_FILE_BYTES)
        assertEquals(TestTemplateStore.MAX_ITEMS, TestTemplateArchive.decode(text).size)
    }

    @Test fun `invalid incoming item cannot partially save valid neighbor`() {
        val items = sample() + TestTemplate("invalid", " ", TestAction.Voice("1"))
        assertThrows(IllegalArgumentException::class.java) { store.importItems(items) }
        assertTrue(store.list().isEmpty())
    }
}
