package com.example.testdialer.legacy

import com.example.testdialer.VoiceTestResult
import com.example.testdialer.domain.EventId
import com.example.testdialer.domain.ObservationSource
import com.example.testdialer.domain.ObservationStatus
import com.example.testdialer.domain.RunId
import com.example.testdialer.domain.StepId
import com.example.testdialer.domain.TestAction
import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceTestResultDomainAdapterTest {
    @Test
    fun mapsSuccessWithoutInferringCallState() {
        val event = result(VoiceTestResult.Outcome.SUCCESS).toDomainTestEvent(RUN_ID, STEP_ID)

        assertEquals(ObservationStatus.CONFIRMED, event.observation?.status)
        assertEquals(ObservationSource.TESTER, event.observation?.source)
        assertEquals("LEGACY_SUCCESS", event.observation?.code)
    }

    @Test
    fun mapsFailureWithoutInferringCallState() {
        val event = result(VoiceTestResult.Outcome.FAILURE).toDomainTestEvent(RUN_ID, STEP_ID)

        assertEquals(ObservationStatus.NOT_CONFIRMED, event.observation?.status)
        assertEquals(ObservationSource.TESTER, event.observation?.source)
        assertEquals("LEGACY_FAILURE", event.observation?.code)
    }

    @Test
    fun mapsNotCheckedToNeutralNotVerifiedTesterObservation() {
        val event = result(VoiceTestResult.Outcome.NOT_CHECKED).toDomainTestEvent(RUN_ID, STEP_ID)

        assertEquals(ObservationStatus.NOT_VERIFIED, event.observation?.status)
        assertEquals("NOT_VERIFIED", event.observation?.code)
    }

    @Test
    fun preservesLegacyIdentityTimeNumberAndName() {
        val event = result(VoiceTestResult.Outcome.SUCCESS).toDomainTestEvent(RUN_ID, STEP_ID)

        assertEquals(EventId("voice-id"), event.id)
        assertEquals(1_234_567L, event.occurredAtMillis)
        assertEquals("+48123123123", (event.action as TestAction.Voice).destination)
        assertEquals("+48123123123", event.correlation.destinationAddress)
        assertEquals("legacy.voice.test_name", event.correlation.references.single().namespace)
        assertEquals("International voice", event.correlation.references.single().value)
    }

    private fun result(outcome: VoiceTestResult.Outcome) = VoiceTestResult(
        id = "voice-id",
        outcome = outcome,
        timestampMillis = 1_234_567L,
        phoneNumber = "+48123123123",
        testName = "International voice",
    )

    private companion object {
        val RUN_ID = RunId("legacy-run")
        val STEP_ID = StepId("legacy-voice-step")
    }
}
