package com.example.testdialer.sms

import android.content.Intent
import android.net.Uri

object SmsComposerIntentFactory {
    fun create(destination: String, message: String): Intent {
        val trimmedDestination = destination.trim()
        require(trimmedDestination.isNotBlank()) { "SMS destination must not be blank" }
        require(message.isNotBlank()) { "SMS message must not be blank" }
        require(trimmedDestination.none { it.isISOControl() }) {
            "SMS destination must not contain control characters"
        }

        return Intent(
            Intent.ACTION_SENDTO,
            Uri.fromParts("smsto", trimmedDestination, null),
        ).putExtra("sms_body", message)
    }
}
