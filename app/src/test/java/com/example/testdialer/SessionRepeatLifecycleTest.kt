package com.example.testdialer

import android.content.res.Configuration
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import com.example.testdialer.active.LocalScenario
import com.example.testdialer.active.SessionRepeatState
import com.example.testdialer.active.SessionRepeatViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-mdpi")
class SessionRepeatLifecycleTest {
    @Test
    fun `ready repeat plan survives activity recreation without loading again`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        val viewModelField = MainActivity::class.java
            .getDeclaredField("sessionRepeatViewModel")
            .apply { isAccessible = true }
        val viewModel = viewModelField.get(activity) as SessionRepeatViewModel
        val stateField = SessionRepeatViewModel::class.java
            .getDeclaredField("_state")
            .apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val state = stateField.get(viewModel) as MutableLiveData<SessionRepeatState>
        state.value = SessionRepeatState.Ready(
            LocalScenario("repeat", "Powtórka: sesja kontrolna", emptyList()),
        )
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        controller.configurationChange(Configuration(activity.resources.configuration).apply {
            orientation = Configuration.ORIENTATION_LANDSCAPE
        })
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val restored = controller.get()
        val restoredViewModel = viewModelField.get(restored) as SessionRepeatViewModel
        val dialogField = MainActivity::class.java
            .getDeclaredField("sessionRepeatDialog")
            .apply { isAccessible = true }
        val dialog = dialogField.get(restored) as android.app.AlertDialog
        assertTrue(restoredViewModel === viewModel)
        assertTrue(restoredViewModel.state.value is SessionRepeatState.Ready)
        assertTrue(dialog.isShowing)
        assertEquals(
            "Plan powtórki",
            dialog.findViewById<android.widget.TextView>(
                android.content.res.Resources.getSystem().getIdentifier(
                    "alertTitle",
                    "id",
                    "android",
                ),
            )?.text?.toString(),
        )

        dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertEquals(SessionRepeatState.Idle, restoredViewModel.state.value)
        controller.pause().stop().destroy()
    }
}
