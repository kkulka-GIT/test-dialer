package com.example.testdialer

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.example.testdialer.active.ActiveRunViewModel
import com.example.testdialer.domain.TestRunStatus
import com.example.testdialer.domain.execution.TimelineEntryKind
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowAlertDialog
import java.io.File
import java.util.concurrent.Executors

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RunCompletionUiTest {
    @Test fun `unfinished tests require confirmation and cancellation preserves the session`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        val model = model(activity)
        button(activity, activity.getString(R.string.run_start_scenario)).performClick()
        await { model.state.value?.active != null && model.state.value?.busy == false }
        val initial = model.state.value!!.active!!
        model.skip(initial.tasks[1].step.id)
        await { model.state.value?.busy == false }
        val before = model.state.value!!.active!!

        button(activity, activity.getString(R.string.run_complete)).performClick()
        val dialog = ShadowAlertDialog.getLatestAlertDialog()
        assertTrue(dialog.isShowing)
        val message = dialog.findViewById<TextView>(android.R.id.message).text.toString()
        assertTrue(message.contains("Niewykonane testy: 2"))
        assertTrue(message.contains("Voice krajowy"))
        assertTrue(message.contains("Data HTTPS"))
        assertFalse(message.contains("SMS standard"))
        capture(dialog)
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
        assertEquals(before, model.state.value!!.active)
        assertFalse(model.state.value!!.busy)
        assertNull(Shadows.shadowOf(activity).nextStartedActivity)

        button(activity, activity.getString(R.string.run_complete)).performClick()
        ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        await { model.state.value?.active == null && model.state.value?.busy == false }
        val worker = Executors.newSingleThreadExecutor()
        val stored = try {
            worker.submit<com.example.testdialer.persistence.StoredTestRun> {
                (activity.application as TestDialerApplication).testRunRepository.get(before.stored.run.id)!!
            }.get()
        } finally { worker.shutdownNow() }
        assertEquals(TestRunStatus.COMPLETED, stored.run.status)
        assertEquals(before.stored.run.events, stored.run.events)
        assertEquals(before.stored.run.timeline, stored.run.timeline.dropLast(1))
        assertEquals(TimelineEntryKind.RUN_COMPLETED, stored.run.timeline.last().kind)
        assertNull(Shadows.shadowOf(activity).nextStartedActivity)
        controller.pause().stop().destroy()
    }

    @Test fun `session without planned tests completes without extra confirmation`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        val model = model(activity)
        button(activity, activity.getString(R.string.run_start_empty)).performClick()
        await { model.state.value?.active != null && model.state.value?.busy == false }
        val previousDialog = ShadowAlertDialog.getLatestAlertDialog()
        button(activity, activity.getString(R.string.run_complete)).performClick()
        await { model.state.value?.active == null && model.state.value?.busy == false }
        assertSame(previousDialog, ShadowAlertDialog.getLatestAlertDialog())
        assertNull(Shadows.shadowOf(activity).nextStartedActivity)
        controller.pause().stop().destroy()
    }

    private fun model(activity: MainActivity) = MainActivity::class.java.getDeclaredField("activeRunViewModel")
        .apply { isAccessible = true }.get(activity) as ActiveRunViewModel

    private fun await(condition: () -> Boolean) {
        repeat(300) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            Thread.sleep(10)
        }
        error("Timed out waiting for session state")
    }

    private fun views(view: View): Sequence<View> = sequence {
        yield(view)
        if (view is ViewGroup) for (index in 0 until view.childCount) yieldAll(views(view.getChildAt(index)))
    }

    private fun button(activity: MainActivity, text: String) = views(activity.findViewById(android.R.id.content))
        .filterIsInstance<Button>().first { it.text.toString() == text }

    private fun capture(dialog: AlertDialog) {
        val root = dialog.window!!.decorView
        root.measure(View.MeasureSpec.makeMeasureSpec(360, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.AT_MOST))
        root.layout(0, 0, 360, root.measuredHeight)
        val bitmap = Bitmap.createBitmap(360, root.height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bitmap))
        val file = File("build/reports/screenshots/run-completion-pending.png").apply { parentFile!!.mkdirs() }
        file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        bitmap.recycle()
    }
}
