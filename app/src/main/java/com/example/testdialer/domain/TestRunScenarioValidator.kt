package com.example.testdialer.domain

data class TestRunValidationIssue(
    val eventId: EventId?,
    val message: String,
)

object TestRunScenarioValidator {
    fun validate(
        run: TestRun,
        scenario: ScenarioDefinition,
    ): List<TestRunValidationIssue> {
        val issues = mutableListOf<TestRunValidationIssue>()

        if (run.scenarioId != scenario.id) {
            issues += TestRunValidationIssue(
                eventId = null,
                message = "Run scenario identifier does not match the definition",
            )
        }
        if (run.scenarioVersion != scenario.version) {
            issues += TestRunValidationIssue(
                eventId = null,
                message = "Run scenario version does not match the definition",
            )
        }

        val stepsById = scenario.steps.associateBy { it.id }
        run.events.forEach { event ->
            val step = stepsById[event.stepId]
            when {
                step == null -> issues += TestRunValidationIssue(
                    eventId = event.id,
                    message = "Event references a step outside the scenario definition",
                )
                event.action != step.action -> issues += TestRunValidationIssue(
                    eventId = event.id,
                    message = "Event action does not match its scenario step",
                )
            }
        }

        return issues
    }

    fun requireValid(
        run: TestRun,
        scenario: ScenarioDefinition,
    ) {
        val issues = validate(run, scenario)
        require(issues.isEmpty()) {
            issues.joinToString(separator = "; ") { it.message }
        }
    }
}
