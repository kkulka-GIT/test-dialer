package com.example.testdialer.legacy

import com.example.testdialer.VoiceTestResult
import com.example.testdialer.domain.CorrelationMetadata
import com.example.testdialer.domain.CorrelationReference
import com.example.testdialer.domain.EventId
import com.example.testdialer.domain.Observation
import com.example.testdialer.domain.ObservationSource
import com.example.testdialer.domain.ObservationStatus
import com.example.testdialer.domain.RunId
import com.example.testdialer.domain.StepId
import com.example.testdialer.domain.TestAction
import com.example.testdialer.domain.TestEvent

fun VoiceTestResult.toDomainTestEvent(
    runId: RunId,
    stepId: StepId,
): TestEvent {
    val observation = when (outcome) {
        VoiceTestResult.Outcome.SUCCESS -> Observation(
            status = ObservationStatus.CONFIRMED,
            source = ObservationSource.TESTER,
            code = "LEGACY_SUCCESS",
        )
        VoiceTestResult.Outcome.FAILURE -> Observation(
            status = ObservationStatus.NOT_CONFIRMED,
            source = ObservationSource.TESTER,
            code = "LEGACY_FAILURE",
        )
        VoiceTestResult.Outcome.NOT_CHECKED -> Observation(
            status = ObservationStatus.NOT_VERIFIED,
            source = ObservationSource.TESTER,
            code = "NOT_VERIFIED",
        )
    }

    val references = buildList {
        testName?.let {
            add(CorrelationReference(namespace = LEGACY_TEST_NAME_NAMESPACE, value = it))
        }
    }

    return TestEvent(
        id = EventId(id),
        runId = runId,
        stepId = stepId,
        action = TestAction.Voice(destination = phoneNumber),
        occurredAtMillis = timestampMillis,
        observation = observation,
        correlation = CorrelationMetadata(
            destinationAddress = phoneNumber,
            references = references,
        ),
    )
}

private const val LEGACY_TEST_NAME_NAMESPACE = "legacy.voice.test_name"
