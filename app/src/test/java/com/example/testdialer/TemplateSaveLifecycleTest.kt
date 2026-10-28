package com.example.testdialer

import android.content.res.Configuration
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import com.example.testdialer.domain.EventId
import com.example.testdialer.domain.TestAction
import com.example.testdialer.templates.TemplateSaveUiState
import com.example.testdialer.templates.TemplateSaveViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-mdpi")
class TemplateSaveLifecycleTest {
    @Test
    fun `template name and busy save survive activity recreation`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        val viewModelField = MainActivity::class.java
            .getDeclaredField("templateSaveViewModel")
            .apply { isAccessible = true }
        val viewModel = viewModelField.get(activity) as TemplateSaveViewModel
        val stateField = TemplateSaveViewModel::class.java
            .getDeclaredField("mutableState")
            .apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val state = stateField.get(viewModel) as MutableLiveData<TemplateSaveUiState>
        state.value = TemplateSaveUiState(
            eventId = EventId("event"),
            action = TestAction.Voice("+48123456789"),
            name = "Połączenie kontrolne",
            selection = 10,
            open = true,
            busy = true,
        )
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        controller.configurationChange(Configuration(activity.resources.configuration).apply {
            orientation = Configuration.ORIENTATION_LANDSCAPE
        })
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val restored = controller.get()
        val restoredViewModel = viewModelField.get(restored) as TemplateSaveViewModel
        val dialogField = MainActivity::class.java
            .getDeclaredField("templateSaveDialog")
            .apply { isAccessible = true }
        val inputField = MainActivity::class.java
            .getDeclaredField("templateSaveInput")
            .apply { isAccessible = true }
        val dialog = dialogField.get(restored) as android.app.AlertDialog
        val input = inputField.get(restored) as android.widget.EditText
        assertTrue(restoredViewModel === viewModel)
        assertTrue(dialog.isShowing)
        assertEquals("Połączenie kontrolne", input.text.toString())
        assertFalse(dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).isEnabled)
        assertFalse(dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).isEnabled)
        controller.pause().stop().destroy()
    }
}
