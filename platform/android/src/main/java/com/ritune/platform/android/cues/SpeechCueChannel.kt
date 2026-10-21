package com.ritune.platform.android.cues

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.ritune.core.models.Cue
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads step names aloud. TextToSpeech binds to the system engine asynchronously on the first request,
 * so the latest request is held until the engine is ready.
 * While speaking it holds transient, may-duck audio focus so the user's music dips instead of stopping.
 */
@Singleton
class SpeechCueChannel @Inject constructor(
    @ApplicationContext private val context: Context,
) : CueChannel {

    private val audioManager = context.getSystemService(AudioManager::class.java)
    private var tts: TextToSpeech? = null
    private var ready = false

    /** Only the latest matters: speaking flushes the queue anyway. */
    private var pending: String? = null

    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(attributes)
        .setOnAudioFocusChangeListener { }
        .build()

    override fun handles(cue: Cue) = cue is Cue.Speech

    override fun play(cue: Cue) {
        if (cue !is Cue.Speech) return
        if (ready) {
            speak(cue.text)
        } else {
            pending = cue.text
            bindEngine()
        }
    }

    // Bound on first use rather than in prepare(), so nothing starts while speech is turned off.
    private fun bindEngine() {
        if (tts != null) return
        lateinit var engine: TextToSpeech
        engine = TextToSpeech(context) { status ->
            // Delivered on the main thread. Ignore an engine that was released before it finished binding.
            if (engine === tts) onInit(engine, status)
        }
        tts = engine
    }

    override fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
        pending = null
        abandonFocus()
    }

    private fun onInit(engine: TextToSpeech, status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            Log.w(TAG, "TextToSpeech init failed ($status)")
            pending = null
            return
        }
        val language = engine.setLanguage(Locale.getDefault())
        if (language == TextToSpeech.LANG_MISSING_DATA || language == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "${Locale.getDefault()} is not available; using the engine default")
        }
        engine.setAudioAttributes(attributes)
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            // These arrive on a binder thread; AudioManager is safe to call from any thread.
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) = abandonFocus()

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) = abandonFocus()
        })
        ready = true
        pending?.let(::speak)
        pending = null
    }

    private fun speak(text: String) {
        val engine = tts ?: return
        requestFocus()
        // QUEUE_FLUSH: after a quick skip, only the current step's name is read.
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    private fun requestFocus() {
        audioManager.requestAudioFocus(focusRequest)
    }

    private fun abandonFocus() {
        audioManager.abandonAudioFocusRequest(focusRequest)
    }

    private companion object {
        const val TAG = "SpeechCueChannel"
        const val UTTERANCE_ID = "ritune-step"
    }
}
