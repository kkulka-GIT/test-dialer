package com.example.testdialer.active

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.testdialer.domain.ScenarioStepDefinition
import com.example.testdialer.domain.StepId
import com.example.testdialer.domain.TestAction
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ScenarioPlanArchiveTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    @Before fun clear() { context.getSharedPreferences(ScenarioPlanStore.PREFERENCES, Context.MODE_PRIVATE).edit().clear().commit() }

    private fun plan(name: String, target: String = "123"): SavedScenarioPlan {
        val data = StepId("data")
        return SavedScenarioPlan("source-$name", name, LocalScenario("scenario-$name", name, listOf(
            ScenarioStepDefinition(StepId("sms"), 0, "SMS", "Wyślij", TestAction.Sms(target, "Treść")),
            ScenarioStepDefinition(data, 1, "Dane", "Pobierz", TestAction.Data("https://example.com/file")),
        ), mapOf(data to 100_000_000L)))
    }

    @Test fun `versioned archive round trip preserves parameters but creates portable identities`() {
        val source = listOf(plan("Kontrola"))
        val text = ScenarioPlanArchive.encode(source)
        val decoded = ScenarioPlanArchive.read(ByteArrayInputStream(text.toByteArray()))
        assertEquals(source.single().name, decoded.single().name)
        assertEquals(source.single().scenario.steps.map { it.action }, decoded.single().scenario.steps.map { it.action })
        assertEquals(100_000_000L, decoded.single().scenario.dataAmounts[decoded.single().scenario.steps[1].id])
        assertNotEquals(source.single().id, decoded.single().id)
    }

    @Test fun `preview and confirmed import deduplicate without replacing existing plans`() {
        val store = ScenarioPlanStore(context)
        store.save("Kontrola", plan("Kontrola").scenario)
        val incoming = listOf(plan("Kontrola"), plan("Nowy", "456"))
        assertEquals(ScenarioPlanImportResult(1, 1), store.previewImport(incoming))
        assertEquals(listOf("Kontrola"), store.list().map { it.name })
        assertEquals(ScenarioPlanImportResult(1, 1), store.importItems(incoming))
        assertEquals(listOf("Kontrola", "Nowy"), store.list().map { it.name })
    }

    @Test fun `malformed or unsupported archive is rejected without persistence`() {
        val store = ScenarioPlanStore(context)
        val bad = listOf(
            "{}",
            "{\"format\":\"test-dialer-scenario-plans\",\"schemaVersion\":2,\"plans\":[]}",
            "{\"format\":\"test-dialer-scenario-plans\",\"schemaVersion\":1,\"plans\":[{\"name\":\"X\",\"steps\":[]}]}",
            "{\"format\":\"test-dialer-scenario-plans\",\"schemaVersion\":1,\"plans\":[{\"name\":\"X\",\"steps\":[{\"title\":\"D\",\"instruction\":\"I\",\"type\":\"DATA\",\"target\":\"https://example.com\",\"requiresBytes\":true,\"bytes\":0}]}]}",
        )
        bad.forEach { assertThrows(Exception::class.java) { ScenarioPlanArchive.decode(it) } }
        assertTrue(store.list().isEmpty())
    }

    @Test fun `oversized stream is rejected before decoding`() {
        val bytes = ByteArray(ScenarioPlanArchive.MAX_FILE_BYTES + 1) { 'x'.code.toByte() }
        assertThrows(IllegalArgumentException::class.java) { ScenarioPlanArchive.read(ByteArrayInputStream(bytes)) }
    }

    @Test fun `capacity failure is atomic and preserves all existing plans`() {
        val store = ScenarioPlanStore(context)
        repeat(ScenarioPlanStore.MAX_PLANS) { store.save("Plan $it", plan("Plan $it", "$it").scenario) }
        val before = store.list()
        assertThrows(IllegalArgumentException::class.java) { store.importItems(listOf(plan("Nadmiar", "999"))) }
        assertEquals(before, store.list())
    }
}
