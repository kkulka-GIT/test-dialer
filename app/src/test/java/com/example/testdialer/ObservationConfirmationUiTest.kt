package com.example.testdialer

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
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
    @Test fun `Data cancellation requires confirmation and keep leaves transfer running`() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        button(activity, activity.getString(R.string.data_type)).performClick()
        MainActivity::class.java.getDeclaredField("cellularDataState").apply {
            isAccessible = true
            set(activity, com.example.testdialer.data.CellularDataUiState(
                busy = true,
                bytes = 25_000_000,
                targetBytes = 100_000_000,
            ))
        }
        renderScenario(activity, "DATA")

        button(activity, activity.getString(R.string.data_cancel)).performClick()
        val dialog = ShadowAlertDialog.getLatestAlertDialog()
        val message = dialog.findViewById<TextView>(android.R.id.message).text.toString()
        assertTrue(message.contains("25000000"))
        assertTrue(message.contains("100000000"))
        assertTrue(message.contains("częściowy wynik"))
        capture(dialog, "data-cancel-confirm.png")
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick()

        val state = MainActivity::class.java.getDeclaredField("cellularDataState").apply { isAccessible = true }
            .get(activity) as com.example.testdialer.data.CellularDataUiState
        assertTrue(state.busy && !state.saved)
        assertTrue(button(activity, activity.getString(R.string.data_cancel)).isEnabled)
    }

    @Test fun `Data transfer requires confirmation and cancel does not start it`() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        button(activity, activity.getString(R.string.data_type)).performClick()
        val amount = views(activity.findViewById(android.R.id.content)).filterIsInstance<EditText>()
            .first { it.hint == activity.getString(R.string.data_amount_hint) }
        amount.setText("100")

        button(activity, activity.getString(R.string.data_start)).performClick()
        val dialog = ShadowAlertDialog.getLatestAlertDialog()
        val message = dialog.findViewById<TextView>(android.R.id.message).text.toString()
        assertTrue(message.contains("100 MB"))
        assertTrue(message.contains("HTTPS", ignoreCase = true))
        capture(dialog, "data-start-confirm.png")
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick()

        val state = MainActivity::class.java.getDeclaredField("cellularDataState").apply { isAccessible = true }
            .get(activity) as com.example.testdialer.data.CellularDataUiState
        assertTrue(!state.busy && !state.saved)
        assertTrue(button(activity, activity.getString(R.string.data_start)).isEnabled)
    }

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

    private fun renderScenario(activity: MainActivity, typeName: String) {
        val typeClass = Class.forName("com.example.testdialer.ui.TestType")
        val type = typeClass.enumConstants.first { (it as Enum<*>).name == typeName }
        MainActivity::class.java.getDeclaredMethod("renderScenario", typeClass).apply {
            isAccessible = true
            invoke(activity, type)
        }
    }

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
