package com.example.testdialer

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import com.example.testdialer.templates.TestTemplateStore
import com.example.testdialer.domain.TestAction
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowAlertDialog
import androidx.test.core.app.ApplicationProvider
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EvolutionWorkflowUiTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    @Before fun clear() {
        context.getSharedPreferences("ui-settings", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("test-templates-v1", Context.MODE_PRIVATE).edit().clear().commit()
    }
    @After fun cleanup() { clear() }

    @Test fun `saved template opens a new editable SMS test without sending`() {
        TestTemplateStore(context).save("SMS kontrolny", TestAction.Sms("+48123456789", "Treść kontrolna"))
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        button(activity, activity.getString(R.string.run_add_test)).performClick()
        val add = ShadowAlertDialog.getLatestAlertDialog()
        add.listView.performItemClick(null, 3, 3)
        val picker = ShadowAlertDialog.getLatestAlertDialog()
        picker.listView.performItemClick(null, 0, 0)
        val use = ShadowAlertDialog.getLatestAlertDialog()
        use.listView.performItemClick(null, 0, 0)
        await { views(activity.findViewById(android.R.id.content)).filterIsInstance<EditText>().any { it.text.toString() == "Treść kontrolna" && it.isShown } }
        val edits = views(activity.findViewById(android.R.id.content)).filterIsInstance<EditText>().map { it.text.toString() }.toList()
        assertTrue(edits.contains("+48123456789"))
        assertNull(Shadows.shadowOf(activity).nextStartedActivity)
        capture(activity, "evolution-template-sms")
        button(activity, "Wróć do sesji i listy testów").performClick()
        assertTrue(button(activity, activity.getString(R.string.run_complete)).isShown)
        val preserved = views(activity.findViewById(android.R.id.content)).filterIsInstance<EditText>()
            .first { it.hint == activity.getString(R.string.sms_message_hint) }
        assertEquals("Treść kontrolna", preserved.text.toString())

        controller.pause().stop().destroy()
    }

    @Test fun `search preserves typing focus and query across recreation`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        button(activity, activity.getString(R.string.nav_register)).performClick()
        val search = views(activity.findViewById(android.R.id.content)).filterIsInstance<EditText>().first { it.hint == "Szukaj nazwy sesji lub ID" }
        search.requestFocus()
        search.setText("moja sesja")
        assertTrue(search.hasFocus())
        assertTrue(search.isShown)
        controller.configurationChange(Configuration())
        val restored = controller.get()
        val restoredSearch = views(restored.findViewById(android.R.id.content)).filterIsInstance<EditText>().first { it.hint == "Szukaj nazwy sesji lub ID" }
        assertEquals("moja sesja", restoredSearch.text.toString())
        capture(restored, "evolution-register-search")
        controller.pause().stop().destroy()
    }

    @Test fun `night theme and large fonts retain readable home actions`() {
        context.getSharedPreferences("ui-settings", Context.MODE_PRIVATE).edit().putInt("theme", 2).commit()
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        var activity = controller.get()
        assertEquals(Configuration.UI_MODE_NIGHT_YES, activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)
        assertEquals(0xFF10191F.toInt(), activity.getColor(R.color.ui_background))
        capture(activity, "evolution-home-dark")
        controller.configurationChange(Configuration(activity.resources.configuration).apply { fontScale = 1.6f })
        activity = controller.get()
        capture(activity, "evolution-home-dark-large")
        views(activity.findViewById(android.R.id.content)).filterIsInstance<Button>().filter { it.isShown && it.width > 0 }.forEach { control ->
            control.layout?.let { layout -> if (layout.lineCount > 0) assertTrue("Clipped: ${control.text}", layout.getLineBottom(layout.lineCount - 1) <= control.height - control.compoundPaddingTop - control.compoundPaddingBottom) }
        }
        controller.pause().stop().destroy()
    }

    private fun button(activity: MainActivity, label: String) = views(activity.findViewById(android.R.id.content)).filterIsInstance<Button>().first { it.text.toString() == label }
    private fun views(view: View): Sequence<View> = sequence {
        yield(view)
        if (view is ViewGroup) repeat(view.childCount) { yieldAll(views(view.getChildAt(it))) }
    }
    private fun await(condition: () -> Boolean) {
        repeat(200) { Shadows.shadowOf(Looper.getMainLooper()).idle(); if (condition()) return; Thread.sleep(5) }
        assertTrue("UI did not reach expected state", condition())
    }
    private fun capture(activity: MainActivity, name: String) {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val root = activity.findViewById<View>(android.R.id.content)
        root.measure(View.MeasureSpec.makeMeasureSpec(360, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY))
        root.layout(0, 0, 360, 800)
        val bitmap = Bitmap.createBitmap(360, 800, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bitmap))
        val file = File("build/reports/screenshots/$name.png").apply { parentFile.mkdirs() }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
