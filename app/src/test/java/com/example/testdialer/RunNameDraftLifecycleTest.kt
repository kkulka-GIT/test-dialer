package com.example.testdialer

import android.content.res.Configuration
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-mdpi")
class RunNameDraftLifecycleTest {
    @Test
    fun `run name draft and cursor survive activity recreation`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        findInput(activity, activity.getString(R.string.run_name_hint)).apply {
            setText("Kontrola poranna")
            setSelection(8)
        }

        controller.configurationChange(Configuration(activity.resources.configuration).apply {
            orientation = Configuration.ORIENTATION_LANDSCAPE
        })

        val restoredActivity = controller.get()
        val restored = findInput(restoredActivity, restoredActivity.getString(R.string.run_name_hint))
        assertEquals("Kontrola poranna", restored.text.toString())
        assertEquals(8, restored.selectionStart)
        controller.pause().stop().destroy()
    }

    @Test
    fun `successful run start clears previous name draft`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        findInput(activity, activity.getString(R.string.run_name_hint)).setText("Kontrola poranna")
        findButton(activity, activity.getString(R.string.run_start_empty)).performClick()

        val field = MainActivity::class.java.getDeclaredField("runNameDraft").apply {
            isAccessible = true
        }
        val deadline = System.currentTimeMillis() + 3_000
        while (field.get(activity) != "" && System.currentTimeMillis() < deadline) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }

        assertEquals("", field.get(activity))
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
