package com.example.testdialer.domain.execution

import com.example.testdialer.domain.AttemptId
import com.example.testdialer.domain.EventId
import com.example.testdialer.domain.RunId
import com.example.testdialer.domain.TimelineEntryId
import java.util.UUID

fun interface TimeProvider {
    fun capture(): CapturedTime
}

fun interface RunIdProvider {
    fun next(): RunId
}

fun interface EventIdProvider {
    fun next(): EventId
}

fun interface AttemptIdProvider {
    fun next(): AttemptId
}

fun interface TimelineEntryIdProvider {
    fun next(): TimelineEntryId
}

object SystemTimeProvider : TimeProvider {
    override fun capture() = CapturedTime(
        epochMillis = System.currentTimeMillis(),
        monotonicNanos = System.nanoTime(),
    )
}

object UuidRunIdProvider : RunIdProvider {
    override fun next() = RunId(UUID.randomUUID().toString())
}

object UuidEventIdProvider : EventIdProvider {
    override fun next() = EventId(UUID.randomUUID().toString())
}

object UuidAttemptIdProvider : AttemptIdProvider {
    override fun next() = AttemptId(UUID.randomUUID().toString())
}

object UuidTimelineEntryIdProvider : TimelineEntryIdProvider {
    override fun next() = TimelineEntryId(UUID.randomUUID().toString())
}
