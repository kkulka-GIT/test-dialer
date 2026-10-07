package com.example.testdialer

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowAlertDialog
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ObservationConfirmationUiTest {
    @Test fun `Voice confirmation is readable on a narrow phone`() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        MainActivity::class.java.getDeclaredField("pendingPhoneNumber").apply {
            isAccessible = true
            set(activity, "+48123123123")
        }
        MainActivity::class.java.getDeclaredField("awaitingVoiceOutcome").apply {
            isAccessible = true
            setBoolean(activity, true)
        }
        val typeClass = Class.forName("com.example.testdialer.ui.TestType")
        val voice = typeClass.enumConstants.first { (it as Enum<*>).name == "VOICE" }
        MainActivity::class.java.getDeclaredMethod("renderScenario", typeClass).apply {
            isAccessible = true
            invoke(activity, voice)
        }

        button(activity, activity.getString(R.string.outcome_success)).performClick()
        val dialog = ShadowAlertDialog.getLatestAlertDialog()
        val message = dialog.findViewById<TextView>(android.R.id.message).text.toString()
        assertTrue(message.contains("+48123123123"))
        assertTrue(message.contains(activity.getString(R.string.outcome_success)))
        capture(dialog, "voice-observation-confirm.png")
    }

    private fun button(activity: MainActivity, text: String) = views(activity.findViewById(android.R.id.content))
        .filterIsInstance<Button>().first { it.text.toString() == text }

    private fun views(view: View): Sequence<View> = sequence {
        yield(view)
        if (view is ViewGroup) for (index in 0 until view.childCount) yieldAll(views(view.getChildAt(index)))
    }

    private fun capture(dialog: AlertDialog, name: String) {
        val root = dialog.window!!.decorView
        root.measure(
            View.MeasureSpec.makeMeasureSpec(360, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.AT_MOST),
        )
        root.layout(0, 0, 360, root.measuredHeight)
        val bitmap = Bitmap.createBitmap(360, root.height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bitmap))
        val file = File("build/reports/screenshots/$name").apply { parentFile!!.mkdirs() }
        file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        bitmap.recycle()
    }
}
