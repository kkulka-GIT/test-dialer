package com.example.testdialer.domain

data class ScenarioStepDefinition(
    val id: StepId,
    val order: Int,
    val title: String,
    val instruction: String,
    val action: TestAction,
    val expectedResult: ExpectedResult? = null,
) {
    init {
        require(order >= 0) { "Step order must be non-negative" }
        require(title.isNotBlank()) { "Step title must not be blank" }
        require(instruction.isNotBlank()) { "Step instruction must not be blank" }
    }
}

data class ScenarioDefinition(
    val id: ScenarioId,
    val version: Int,
    val name: String,
    val description: String? = null,
    val steps: List<ScenarioStepDefinition>,
) {
    init {
        require(version > 0) { "Scenario version must be positive" }
        require(name.isNotBlank()) { "Scenario name must not be blank" }
        require(description == null || description.isNotBlank()) {
            "Scenario description must be null or non-blank"
        }
        require(steps.isNotEmpty()) { "Scenario must contain at least one step" }
        require(steps.map { it.id }.distinct().size == steps.size) {
            "Scenario step identifiers must be unique"
        }
        require(steps.map { it.order }.distinct().size == steps.size) {
            "Scenario step order values must be unique"
        }
        require(steps.zipWithNext().all { (previous, next) -> previous.order < next.order }) {
            "Scenario steps must be ordered by ascending order value"
        }
    }
}
