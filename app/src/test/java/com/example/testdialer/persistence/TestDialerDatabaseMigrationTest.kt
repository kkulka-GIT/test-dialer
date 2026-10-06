package com.example.testdialer.persistence

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TestDialerDatabaseMigrationTest {
    private val databaseName = "migration-${UUID.randomUUID()}.db"

    @After
    fun cleanUp() {
        ApplicationProvider.getApplicationContext<Context>().deleteDatabase(databaseName)
    }

    @Test
    fun migrationOneToTwoPreservesHistoryAndCreatesEmptyAnnotationTables() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(databaseName), null).apply {
            VERSION_ONE_SCHEMA.forEach(::execSQL)
            execSQL("INSERT INTO scenarios VALUES ('scenario-1', 1, 'Existing scenario', 'before update')")
            execSQL(
                "INSERT INTO test_runs VALUES ('run-1', 'scenario-1', 1, 'COMPLETED', 1000, 2000, 0)",
            )
            execSQL(
                """INSERT INTO test_events VALUES (
                    'event-1', 'run-1', 'step-1', 0, 'VOICE', '+48123123123', NULL, 1500,
                    'CONFIRMED', 'TESTER', 'MANUAL_OK', 'Preserved observation',
                    NULL, '+48123123123', NULL
                )""".trimIndent(),
            )
            version = 1
        }.close()

        val migrated = Room.databaseBuilder(
            context,
            TestDialerDatabase::class.java,
            databaseName,
        ).addMigrations(TestDialerDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()
        try {
            val snapshot = requireNotNull(migrated.testRunDao().loadSnapshot("run-1"))
            assertEquals("COMPLETED", snapshot.run.status)
            assertEquals("Preserved observation", snapshot.events.single().observationDescription)
            assertNull(migrated.testRunDao().loadAnnotations("run-1").note)
            assertEquals(emptyList<BillingReviewEntity>(), migrated.testRunDao().findBillingReviews("run-1"))
            assertNull(migrated.testRunDao().loadAnnotations("run-1").interruption)
        } finally {
            migrated.close()
        }
    }

    companion object {
        /** Exact DDL exported by Room schema 1; opening schema 2 performs Room's full validation. */
        private val VERSION_ONE_SCHEMA = listOf(
            "CREATE TABLE IF NOT EXISTS `scenarios` (`scenarioId` TEXT NOT NULL, `version` INTEGER NOT NULL, `name` TEXT NOT NULL, `description` TEXT, PRIMARY KEY(`scenarioId`, `version`))",
            "CREATE TABLE IF NOT EXISTS `scenario_steps` (`scenarioId` TEXT NOT NULL, `scenarioVersion` INTEGER NOT NULL, `stepId` TEXT NOT NULL, `stepOrder` INTEGER NOT NULL, `title` TEXT NOT NULL, `instruction` TEXT NOT NULL, `actionKind` TEXT NOT NULL, `actionDestinationOrTarget` TEXT NOT NULL, `actionMessage` TEXT, `expectedResultCode` TEXT, `expectedResultDescription` TEXT, PRIMARY KEY(`scenarioId`, `scenarioVersion`, `stepId`), FOREIGN KEY(`scenarioId`, `scenarioVersion`) REFERENCES `scenarios`(`scenarioId`, `version`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_scenario_steps_scenarioId_scenarioVersion` ON `scenario_steps` (`scenarioId`, `scenarioVersion`)",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_scenario_steps_scenarioId_scenarioVersion_stepOrder` ON `scenario_steps` (`scenarioId`, `scenarioVersion`, `stepOrder`)",
            "CREATE TABLE IF NOT EXISTS `test_runs` (`runId` TEXT NOT NULL, `scenarioId` TEXT NOT NULL, `scenarioVersion` INTEGER NOT NULL, `status` TEXT NOT NULL, `startedAtMillis` INTEGER NOT NULL, `completedAtMillis` INTEGER, `revision` INTEGER NOT NULL, PRIMARY KEY(`runId`), FOREIGN KEY(`scenarioId`, `scenarioVersion`) REFERENCES `scenarios`(`scenarioId`, `version`) ON UPDATE NO ACTION ON DELETE NO ACTION )",
            "CREATE INDEX IF NOT EXISTS `index_test_runs_scenarioId_scenarioVersion` ON `test_runs` (`scenarioId`, `scenarioVersion`)",
            "CREATE TABLE IF NOT EXISTS `test_events` (`eventId` TEXT NOT NULL, `runId` TEXT NOT NULL, `stepId` TEXT NOT NULL, `eventOrder` INTEGER NOT NULL, `actionKind` TEXT NOT NULL, `actionDestinationOrTarget` TEXT NOT NULL, `actionMessage` TEXT, `occurredAtMillis` INTEGER NOT NULL, `observationStatus` TEXT, `observationSource` TEXT, `observationCode` TEXT, `observationDescription` TEXT, `correlationSourceAddress` TEXT, `correlationDestinationAddress` TEXT, `correlationSubscriberAlias` TEXT, PRIMARY KEY(`eventId`), FOREIGN KEY(`runId`) REFERENCES `test_runs`(`runId`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_test_events_runId` ON `test_events` (`runId`)",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_test_events_eventId_runId` ON `test_events` (`eventId`, `runId`)",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_test_events_runId_eventOrder` ON `test_events` (`runId`, `eventOrder`)",
            "CREATE TABLE IF NOT EXISTS `correlation_references` (`eventId` TEXT NOT NULL, `ordinal` INTEGER NOT NULL, `namespace` TEXT NOT NULL, `value` TEXT NOT NULL, PRIMARY KEY(`eventId`, `ordinal`), FOREIGN KEY(`eventId`) REFERENCES `test_events`(`eventId`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_correlation_references_eventId` ON `correlation_references` (`eventId`)",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_correlation_references_eventId_namespace_value` ON `correlation_references` (`eventId`, `namespace`, `value`)",
            "CREATE TABLE IF NOT EXISTS `timeline_entries` (`timelineEntryId` TEXT NOT NULL, `runId` TEXT NOT NULL, `sequenceNumber` INTEGER NOT NULL, `kind` TEXT NOT NULL, `epochMillis` INTEGER NOT NULL, `monotonicNanos` INTEGER NOT NULL, `stepId` TEXT, `attemptId` TEXT, `relatedEventId` TEXT, PRIMARY KEY(`timelineEntryId`), FOREIGN KEY(`runId`) REFERENCES `test_runs`(`runId`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`relatedEventId`, `runId`) REFERENCES `test_events`(`eventId`, `runId`) ON UPDATE NO ACTION ON DELETE NO ACTION )",
            "CREATE INDEX IF NOT EXISTS `index_timeline_entries_runId` ON `timeline_entries` (`runId`)",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_timeline_entries_runId_sequenceNumber` ON `timeline_entries` (`runId`, `sequenceNumber`)",
            "CREATE INDEX IF NOT EXISTS `index_timeline_entries_relatedEventId_runId` ON `timeline_entries` (`relatedEventId`, `runId`)",
        )
    }
}
