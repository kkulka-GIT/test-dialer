package com.example.testdialer.active

import com.example.testdialer.domain.*
import com.example.testdialer.persistence.StoredTestRun
import java.util.UUID

/** Reuses recorded parameters in recorded order, never observations, billing reviews or old IDs. */
object SessionRepeatPlan {
    const val MAX_STEPS = 50
    fun from(stored: StoredTestRun): LocalScenario {
        val events = stored.run.events
        require(events.isNotEmpty()) { "Sesja nie ma zapisanych zdarzeń do powtórzenia." }
        require(events.size <= MAX_STEPS) { "Powtórka obsługuje do $MAX_STEPS zdarzeń. Wybierz krótszą sesję." }
        val amounts = mutableMapOf<StepId, Long?>()
        val steps = events.mapIndexed { index, event ->
            val id = StepId("repeat-${UUID.randomUUID()}")
            val type = when(event.action) { is TestAction.Voice -> "Połączenie"; is TestAction.Sms -> "SMS"; is TestAction.Data -> "Dane" }
            val detail = if (event.action is TestAction.Data) {
                val bytes = event.correlation.references.filter { it.namespace == "requestedBytes" }.singleOrNull()
                    ?.value?.toLongOrNull()?.takeIf { it in 1..com.example.testdialer.data.DataVolume.MAX_BYTES }
                amounts[id] = bytes
                if (bytes == null) " Brak zapisanej ilości danych: podaj ją w formularzu." else " Żądana ilość danych: $bytes B."
            } else ""
            ScenarioStepDefinition(id, index, "${index + 1}. $type", "Parametry ze zdarzenia poprzedniej sesji. Uruchom usługę jawnie po sprawdzeniu formularza.$detail", event.action)
        }
        return LocalScenario("repeat-${UUID.randomUUID()}", "Powtórka: ${stored.scenario.name}".take(160), steps, amounts)
    }
}
