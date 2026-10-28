package com.example.testdialer

import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-mdpi")
class ExecutionDraftSelectionLifecycleTest {
    @Test
    fun `Voice draft cursor survives activity recreation`() {
        assertCursorSurvives(
            typeLabel = R.string.voice_type,
            inputHint = R.string.voice_number_hint,
            value = "+48123456789",
            selection = 4,
        )
    }

    @Test
    fun `SMS draft cursor survives activity recreation`() {
        assertCursorSurvives(
            typeLabel = R.string.sms_type,
            inputHint = R.string.sms_message_hint,
            value = "Wiadomość kontrolna",
            selection = 7,
        )
    }

    @Test
    fun `Data draft cursor survives activity recreation`() {
        assertCursorSurvives(
            typeLabel = R.string.data_type,
            inputHint = R.string.data_url_hint,
            value = "https://example.com/test.bin",
            selection = 12,
        )
    }

    private fun assertCursorSurvives(typeLabel: Int, inputHint: Int, value: String, selection: Int) {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        findButton(activity, activity.getString(typeLabel)).performClick()
        findInput(activity, activity.getString(inputHint)).apply {
            setText(value)
            setSelection(selection)
        }

        controller.configurationChange(Configuration(activity.resources.configuration).apply {
            orientation = Configuration.ORIENTATION_LANDSCAPE
        })

        val restored = controller.get()
        val input = findInput(restored, restored.getString(inputHint))
        assertEquals(value, input.text.toString())
        assertEquals(selection, input.selectionStart)
        controller.pause().stop().destroy()
    }

    private fun findButton(activity: MainActivity, label: String): Button =
        descendants(activity.findViewById(android.R.id.content))
            .filterIsInstance<Button>()
            .first { it.text.toString() == label }

    private fun findInput(activity: MainActivity, hint: String): EditText =
        descendants(activity.findViewById(android.R.id.content))
            .filterIsInstance<EditText>()
            .first { it.hint?.toString() == hint }

    private fun descendants(root: View): Sequence<View> = sequence {
        yield(root)
        if (root is ViewGroup) {
            for (index in 0 until root.childCount) yieldAll(descendants(root.getChildAt(index)))
        }
    }
}
