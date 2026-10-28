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
    fun `Voice SMS and Data draft cursors survive activity recreation`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()

        findButton(activity, activity.getString(R.string.voice_type)).performClick()
        val voice = findInput(activity, activity.getString(R.string.voice_number_hint))
        voice.setText("+48123456789")
        voice.setSelection(4)

        findButton(activity, activity.getString(R.string.sms_type)).performClick()
        val sms = findInput(activity, activity.getString(R.string.sms_message_hint))
        sms.setText("Wiadomość kontrolna")
        sms.setSelection(7)

        findButton(activity, activity.getString(R.string.data_type)).performClick()
        val data = findInput(activity, activity.getString(R.string.data_url_hint))
        data.setText("https://example.com/test.bin")
        data.setSelection(12)

        controller.configurationChange(Configuration(activity.resources.configuration).apply {
            orientation = Configuration.ORIENTATION_LANDSCAPE
        })

        val restored = controller.get()
        assertEquals(12, findInput(restored, restored.getString(R.string.data_url_hint)).selectionStart)

        findButton(restored, restored.getString(R.string.sms_type)).performClick()
        assertEquals(7, findInput(restored, restored.getString(R.string.sms_message_hint)).selectionStart)

        findButton(restored, restored.getString(R.string.voice_type)).performClick()
        assertEquals(4, findInput(restored, restored.getString(R.string.voice_number_hint)).selectionStart)
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
