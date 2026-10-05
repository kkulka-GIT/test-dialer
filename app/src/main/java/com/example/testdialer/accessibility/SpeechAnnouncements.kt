package com.example.testdialer.accessibility

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Lazily starts the user's TTS engine only after an explicit opt-in. */
class SpeechAnnouncements(context: Context) {
    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private var engine: TextToSpeech? = null
    private var ready = false
    private var closed = false
    private var pending: String? = null
    var onUnavailable: (() -> Unit)? = null

    fun say(text: String) {
        if (closed || !enabled()) return
        pending = text
        if (ready) { speakPending(); return }
        if (engine == null) engine = TextToSpeech(appContext) { status ->
            handler.post {
                if (closed || engine == null) return@post
                val language = if (status == TextToSpeech.SUCCESS) engine?.setLanguage(Locale.forLanguageTag("pl-PL")) else null
                ready = language != null && language >= TextToSpeech.LANG_AVAILABLE
                if (ready) speakPending() else {
                    pending = null
                    engine?.shutdown()
                    engine = null
                    if (enabled()) onUnavailable?.invoke()
                }
            }
        }
    }

    private fun speakPending() {
        val text = pending ?: return
        pending = null
        if (enabled() && !closed) engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "test-dialer-status")
    }
    fun stop() { pending = null; engine?.stop() }
    fun close() { closed = true; pending = null; handler.removeCallbacksAndMessages(null); engine?.shutdown(); engine = null; onUnavailable = null }
    private fun enabled() = appContext.getSharedPreferences("ui-settings", Context.MODE_PRIVATE).getBoolean("speech", false)
}
