package com.example.testdialer.report

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

object RunReportFiles {
    /** Unique filenames prevent later exports from changing an already shared snapshot. */
    fun shareIntent(context: Context, content: String, json: Boolean): Intent {
        val directory = File(context.cacheDir, "reports").apply { check(isDirectory || mkdirs()) }
        val file = File(directory, "test-dialer-${UUID.randomUUID()}.${if (json) "json" else "txt"}")
        file.writeText(content, Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.reports", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = if (json) "application/json" else "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri("Test Dialer report", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
