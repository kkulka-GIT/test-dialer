package com.example.testdialer.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ScenarioDefinitionTest {
    @Test
    fun supportsVoiceSmsAndDataStepsInDeclaredOrder() {
        val steps = listOf(
            step("voice", 0, TestAction.Voice("+48123123123")),
            step("sms", 1, TestAction.Sms("+49123123123", "test")),
            step("data", 2, TestAction.Data("https://example.test/payload")),
        )

        val scenario = ScenarioDefinition(
            id = ScenarioId("international-services"),
            version = 1,
            name = "International services",
            steps = steps,
        )

        assertEquals(
            listOf(ServiceType.VOICE, ServiceType.SMS, ServiceType.DATA),
            scenario.steps.map { it.action.serviceType },
        )
    }

    @Test
    fun rejectsDuplicateStepIdentifiers() {
        assertThrows(IllegalArgumentException::class.java) {
            ScenarioDefinition(
                id = ScenarioId("duplicate"),
                version = 1,
                name = "Duplicate",
                steps = listOf(
                    step("same", 0, TestAction.Voice("+48111")),
                    step("same", 1, TestAction.Voice("+48222")),
                ),
            )
        }
    }

    @Test
    fun rejectsStepsOutsideAscendingOrder() {
        assertThrows(IllegalArgumentException::class.java) {
            ScenarioDefinition(
                id = ScenarioId("unordered"),
                version = 1,
                name = "Unordered",
                steps = listOf(
                    step("second", 1, TestAction.Voice("+48111")),
                    step("first", 0, TestAction.Voice("+48222")),
                ),
            )
        }
    }

    private fun step(id: String, order: Int, action: TestAction) = ScenarioStepDefinition(
        id = StepId(id),
        order = order,
        title = id,
        instruction = "Perform $id",
        action = action,
    )
}
