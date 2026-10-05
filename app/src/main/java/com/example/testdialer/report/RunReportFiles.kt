package com.example.testdialer.report

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

object RunReportFiles {
    /** Unique filenames prevent later exports from changing an already shared snapshot. */
    fun shareIntent(context: Context, content: String, json: Boolean): Intent =
        shareIntent(context, content, if (json) "json" else "txt")

    fun shareIntent(context: Context, content: String, extension: String): Intent {
        require(extension in setOf("json", "txt", "csv"))
        val directory = File(context.cacheDir, "reports").apply { check(isDirectory || mkdirs()) }
        val file = File(directory, "test-dialer-${UUID.randomUUID()}.${extension}")
        file.writeText(content, Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.reports", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = when (extension) { "json" -> "application/json"; "csv" -> "text/csv"; else -> "text/plain" }
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri("Test Dialer report", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
