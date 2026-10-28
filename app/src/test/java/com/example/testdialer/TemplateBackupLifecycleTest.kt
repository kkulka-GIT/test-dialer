package com.example.testdialer

import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import android.os.Looper
import com.example.testdialer.backup.TemplateBackupState
import com.example.testdialer.backup.TemplateBackupViewModel
import com.example.testdialer.templates.TestTemplateArchive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-mdpi")
class TemplateBackupLifecycleTest {
    @Test
    fun `template import preview survives activity recreation without restarting read`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        activity.getSharedPreferences("test-templates-v1", Context.MODE_PRIVATE).edit().clear().commit()
        val field = MainActivity::class.java.getDeclaredField("templateBackupViewModel").apply {
            isAccessible = true
        }
        val viewModel = field.get(activity) as TemplateBackupViewModel
        val file = File(activity.cacheDir, "template-recreation.json").apply {
            writeText(TestTemplateArchive.encode(emptyList()))
        }

        viewModel.readImport(Uri.fromFile(file))
        await { viewModel.state.value is TemplateBackupState.Preview }
        val firstDialog = ShadowAlertDialog.getLatestAlertDialog()
        assertTrue(firstDialog.isShowing)
        assertEquals(
            "Podgląd importu szablonów",
            firstDialog.findViewById<android.widget.TextView>(
                android.content.res.Resources.getSystem().getIdentifier("alertTitle", "id", "android"),
            )?.text?.toString(),
        )

        controller.configurationChange(Configuration(controller.get().resources.configuration).apply {
            orientation = Configuration.ORIENTATION_LANDSCAPE
        })
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val restored = controller.get()
        val restoredViewModel = field.get(restored) as TemplateBackupViewModel
        val restoredDialog = ShadowAlertDialog.getLatestAlertDialog()
        assertTrue(restoredDialog.isShowing)
        assertTrue(restoredViewModel === viewModel)
        assertTrue(restoredViewModel.state.value is TemplateBackupState.Preview)
        restoredDialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertEquals(TemplateBackupState.Idle, restoredViewModel.state.value)
        controller.pause().stop().destroy()
    }

    private fun await(condition: () -> Boolean) {
        repeat(100) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            Thread.sleep(10)
        }
        error("Condition not met")
    }
}
