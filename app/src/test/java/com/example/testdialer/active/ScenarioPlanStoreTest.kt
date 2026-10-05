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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ScenarioPlanStoreTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    @Before fun clear() { context.getSharedPreferences(ScenarioPlanStore.PREFERENCES, Context.MODE_PRIVATE).edit().clear().commit() }

    @Test fun `plan persists ordered repeated steps and nullable data amounts`() {
        val dataKnown = StepId("d1")
        val dataUnknown = StepId("d2")
        val source = LocalScenario("source", "Źródło", listOf(
            ScenarioStepDefinition(StepId("s1"), 0, "SMS 1", "Pierwszy", TestAction.Sms("123", "A")),
            ScenarioStepDefinition(StepId("s2"), 1, "SMS 2", "Drugi", TestAction.Sms("123", "A")),
            ScenarioStepDefinition(dataKnown, 2, "Dane", "Znane", TestAction.Data("https://example.com/a")),
            ScenarioStepDefinition(dataUnknown, 3, "Dane 2", "Do wyboru", TestAction.Data("https://example.com/b")),
        ), mapOf(dataKnown to 500_000_000L, dataUnknown to null))
        assertTrue(ScenarioPlanStore(context).save(" Plan kontrolny ", source))

        val reopened = ScenarioPlanStore(context)
        val first = reopened.instantiate(reopened.list().single().id)
        val second = reopened.instantiate(reopened.list().single().id)
        assertEquals("Plan kontrolny", first.name)
        assertEquals(source.steps.map { it.action }, first.steps.map { it.action })
        assertEquals(listOf(500_000_000L, null), first.steps.filter { it.action is TestAction.Data }.map { first.dataAmounts[it.id] })
        assertTrue(first.steps.map { it.id }.toSet().intersect(second.steps.map { it.id }.toSet()).isEmpty())
    }

    @Test fun `invalid and oversized plans do not mutate storage`() {
        val store = ScenarioPlanStore(context)
        val one = LocalScenario("one", "One", listOf(ScenarioStepDefinition(StepId("v"), 0, "Voice", "Call", TestAction.Voice("123"))))
        assertThrows(IllegalArgumentException::class.java) { store.save(" ", one) }
        val steps = (0..ScenarioPlanStore.MAX_STEPS).map { ScenarioStepDefinition(StepId("v$it"), it, "Voice", "Call", TestAction.Voice("123")) }
        assertThrows(IllegalArgumentException::class.java) { store.save("Za duży", LocalScenario("large", "Large", steps)) }
        assertTrue(store.list().isEmpty())
    }

    @Test fun `deletion is selective and never touches another plan`() {
        val store = ScenarioPlanStore(context)
        fun plan(id: String) = LocalScenario(id, id, listOf(ScenarioStepDefinition(StepId(id), 0, "Voice", "Call", TestAction.Voice(id))))
        store.save("Pierwszy", plan("1")); store.save("Drugi", plan("2"))
        assertTrue(store.delete(store.list().first().id))
        assertEquals(listOf("Drugi"), ScenarioPlanStore(context).list().map { it.name })
    }
}
