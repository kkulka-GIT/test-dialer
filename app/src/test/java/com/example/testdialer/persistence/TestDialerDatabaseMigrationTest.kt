package com.example.testdialer.persistence

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TestDialerDatabaseMigrationTest {
    private val databaseName = "migration-${UUID.randomUUID()}.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TestDialerDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory(),
    )

    @After
    fun cleanUp() {
        ApplicationProvider.getApplicationContext<Context>().deleteDatabase(databaseName)
    }

    @Test
    fun migrationOneToTwoPreservesHistoryAndCreatesEmptyAnnotationTables() {
        helper.createDatabase(databaseName, 1).apply {
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
            close()
        }

        helper.runMigrationsAndValidate(
            databaseName,
            2,
            true,
            TestDialerDatabase.MIGRATION_1_2,
        ).close()

        val migrated = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
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
}
