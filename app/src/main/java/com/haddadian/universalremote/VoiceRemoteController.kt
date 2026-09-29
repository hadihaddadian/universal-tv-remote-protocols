package com.haddadian.universalremote

import android.content.Context
import kotlinx.coroutines.CoroutineScope

class VoiceRemoteController(
    context: Context,
    private val engine: AndroidTvRemoteEngine,
    scope: CoroutineScope
) {
    private val voice = AndroidTvVoiceEngine { msg ->
        engine.sendRaw(msg)
    }
    private val capture = VoiceCapture(context, voice, scope)

    suspend fun start(): Result<Unit> = capture.start()
    suspend fun stop() = capture.stop()
}
