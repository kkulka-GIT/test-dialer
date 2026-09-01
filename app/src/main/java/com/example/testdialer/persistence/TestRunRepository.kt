package com.example.testdialer.persistence

import com.example.testdialer.domain.RunId
import com.example.testdialer.domain.ScenarioDefinition
import com.example.testdialer.domain.TestRun
import com.example.testdialer.domain.TestRunStatus

data class TestRunSummary(
    val runId: RunId,
    val scenarioName: String,
    val scenarioVersion: Int,
    val status: TestRunStatus,
    val startedAtMillis: Long,
    val completedAtMillis: Long?,
    val revision: Long,
)

interface TestRunRepository {
    /**
     * Creates a snapshot when [expectedRevision] is null, or replaces the matching revision.
     * Calls are blocking and must be made away from the Android main thread.
     */
    fun saveSnapshot(
        scenario: ScenarioDefinition,
        run: TestRun,
        expectedRevision: Long? = null,
    ): StoredTestRun

    fun get(runId: RunId): StoredTestRun?

    fun listSummaries(): List<TestRunSummary>
}

class RoomTestRunRepository(
    private val dao: TestRunDao,
) : TestRunRepository {
    override fun saveSnapshot(
        scenario: ScenarioDefinition,
        run: TestRun,
        expectedRevision: Long?,
    ): StoredTestRun {
        val snapshot = TestRunPersistenceMapper.toPersistence(
            scenario = scenario,
            run = run,
            revision = expectedRevision ?: 0L,
        )
        val revision = dao.storeSnapshot(snapshot, expectedRevision)
        return StoredTestRun(scenario, run, revision)
    }

    override fun get(runId: RunId): StoredTestRun? =
        dao.loadSnapshot(runId.value)?.let(TestRunPersistenceMapper::fromPersistence)

    override fun listSummaries(): List<TestRunSummary> =
        dao.listRuns().map { run ->
            val scenario = requireNotNull(dao.findScenario(run.scenarioId, run.scenarioVersion)) {
                "Stored run references a missing scenario"
            }
            TestRunSummary(
                runId = RunId(run.runId),
                scenarioName = scenario.name,
                scenarioVersion = run.scenarioVersion,
                status = enumValues<TestRunStatus>().singleOrNull { it.name == run.status }
                    ?: throw IllegalArgumentException("Unknown run status: ${run.status}"),
                startedAtMillis = run.startedAtMillis,
                completedAtMillis = run.completedAtMillis,
                revision = run.revision,
            )
        }
}
