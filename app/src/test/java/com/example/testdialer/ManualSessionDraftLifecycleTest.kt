package com.example.testdialer

import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-mdpi")
class ManualSessionDraftLifecycleTest {
    @Test
    fun `manual session draft and cursors survive activity recreation`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        findInput(activity, activity.getString(R.string.manual_session_name_hint)).apply {
            setText("Kontrola roamingu")
            setSelection(8)
        }
        findInput(activity, activity.getString(R.string.manual_session_target_hint)).apply {
            setText("+48123456789")
            setSelection(4)
        }

        controller.configurationChange(Configuration(activity.resources.configuration).apply {
            orientation = Configuration.ORIENTATION_LANDSCAPE
        })

        val restored = controller.get()
        findInput(restored, restored.getString(R.string.manual_session_name_hint)).let {
            assertEquals("Kontrola roamingu", it.text.toString())
            assertEquals(8, it.selectionStart)
        }
        findInput(restored, restored.getString(R.string.manual_session_target_hint)).let {
            assertEquals("+48123456789", it.text.toString())
            assertEquals(4, it.selectionStart)
        }
        controller.pause().stop().destroy()
    }

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
