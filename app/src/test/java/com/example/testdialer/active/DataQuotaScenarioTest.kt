package com.example.testdialer.active

import com.example.testdialer.domain.TestAction
import com.example.testdialer.data.DataVolume
import org.junit.Assert.*
import org.junit.Test

class DataQuotaScenarioTest {
    @Test fun `data scenario needs no SMS and contains only one data task`() {
        val scenario = LocalScenarioCatalog.dataQuota()
        assertEquals(1, scenario.steps.size)
        assertEquals(0, scenario.steps.single().order)
        assertEquals(TestAction.Data(DataVolume.DEFAULT_URL), scenario.steps.single().action)
    }
}
