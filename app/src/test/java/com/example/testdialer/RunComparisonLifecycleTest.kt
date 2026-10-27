package com.example.testdialer

import android.content.res.Configuration
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import com.example.testdialer.report.ComparisonRow
import com.example.testdialer.report.RunComparison
import com.example.testdialer.report.RunComparisonState
import com.example.testdialer.report.RunComparisonViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-mdpi")
class RunComparisonLifecycleTest {
    @Test
    fun `ready comparison survives activity recreation without loading again`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        val viewModelField = MainActivity::class.java
            .getDeclaredField("runComparisonViewModel")
            .apply { isAccessible = true }
        val viewModel = viewModelField.get(activity) as RunComparisonViewModel
        val stateField = RunComparisonViewModel::class.java
            .getDeclaredField("_state")
            .apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val state = stateField.get(viewModel) as MutableLiveData<RunComparisonState>
        state.value = RunComparisonState.Ready(
            RunComparison(
                baselineId = "baseline",
                comparedId = "compared",
                baselineName = "Sesja A",
                comparedName = "Sesja B",
                parametersMatch = true,
                rows = listOf(ComparisonRow("Liczba zdarzeń", "1", "2")),
            ),
        )
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertTrue(ShadowAlertDialog.getLatestAlertDialog().isShowing)

        controller.configurationChange(Configuration(activity.resources.configuration).apply {
            orientation = Configuration.ORIENTATION_LANDSCAPE
        })
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val restored = controller.get()
        val restoredViewModel = viewModelField.get(restored) as RunComparisonViewModel
        val restoredDialog = ShadowAlertDialog.getLatestAlertDialog()
        assertTrue(restoredViewModel === viewModel)
        assertTrue(restoredViewModel.state.value is RunComparisonState.Ready)
        assertTrue(restoredDialog.isShowing)
        assertEquals(
            "Porównanie sesji",
            restoredDialog.findViewById<android.widget.TextView>(
                android.content.res.Resources.getSystem().getIdentifier(
                    "alertTitle",
                    "id",
                    "android",
                ),
            )?.text?.toString(),
        )

        restoredDialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).performClick()
        assertEquals(RunComparisonState.Idle, restoredViewModel.state.value)
        controller.pause().stop().destroy()
    }
}
