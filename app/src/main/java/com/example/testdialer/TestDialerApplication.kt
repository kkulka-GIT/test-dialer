package com.example.testdialer

import android.app.Application
import com.example.testdialer.persistence.RoomTestRunRepository
import com.example.testdialer.persistence.TestDialerDatabase
import com.example.testdialer.persistence.TestRunRepository

class TestDialerApplication : Application() {
    private val database by lazy { TestDialerDatabase.create(this) }

    val testRunRepository: TestRunRepository by lazy {
        RoomTestRunRepository(database.testRunDao())
    }
}
