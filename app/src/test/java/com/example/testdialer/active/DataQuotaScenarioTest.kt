package com.example.testdialer.active

import com.example.testdialer.domain.TestAction
import com.example.testdialer.data.DataVolume
import org.junit.Assert.*
import org.junit.Test

class DataQuotaScenarioTest {
    @Test fun `quota scenario keeps the same user supplied SMS before and after data`() {
        val scenario = LocalScenarioCatalog.dataQuota(" 1234 ", "ILE")
        assertEquals(listOf(0, 1, 2), scenario.steps.map { it.order })
        assertEquals(TestAction.Sms("1234", "ILE"), scenario.steps[0].action)
        assertEquals(TestAction.Data(DataVolume.DEFAULT_URL), scenario.steps[1].action)
        assertEquals(scenario.steps[0].action, scenario.steps[2].action)
        assertEquals(3, scenario.steps.map { it.id }.distinct().size)
        assertThrows(IllegalArgumentException::class.java) { LocalScenarioCatalog.dataQuota(" ", "ILE") }
    }
}
