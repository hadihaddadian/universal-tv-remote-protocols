package com.haddadian.universalremote

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max

/** Owns Android's microphone resource and streams PCM frames to the TV protocol. */
class VoiceCapture(
    private val context: Context,
    private val voice: AndroidTvVoiceEngine,
    private val scope: CoroutineScope = CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO)
) {
    private var record: AudioRecord? = null
    private var job: Job? = null

    suspend fun start(): Result<Unit> = runCatching {
        stop()
        check(
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        ) { "RECORD_AUDIO permission is not granted" }

        val sampleRate = 8_000
        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        require(minBuffer > 0) { "AudioRecord unavailable" }

        val newRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            max(minBuffer * 2, 4096)
        )
        check(newRecord.state == AudioRecord.STATE_INITIALIZED) {
            newRecord.release()
            "Unable to initialize AudioRecord"
        }

        voice.begin().getOrThrow()
        record = newRecord
        newRecord.startRecording()

        job = scope.launch {
            val buffer = ByteArray(16 * 1024)
            try {
                while (isActive && newRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val count = newRecord.read(buffer, 0, buffer.size)
                    if (count > 0) voice.sendPcm(buffer.copyOf(count)).getOrThrow()
                }
            } finally {
                runCatching { newRecord.stop() }
                runCatching { newRecord.release() }
                if (record === newRecord) record = null
                voice.end()
            }
        }
    }

    suspend fun stop() {
        val runningJob = job
        job = null
        runningJob?.cancelAndJoin()

        val current = record
        record = null
        if (current != null) {
            runCatching { current.stop() }
            runCatching { current.release() }
        }
        voice.end()
    }
}
