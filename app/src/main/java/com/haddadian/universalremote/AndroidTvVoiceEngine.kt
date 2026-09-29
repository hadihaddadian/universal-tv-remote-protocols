package com.haddadian.universalremote

import com.haddadian.universalremote.atv.proto.RemoteMessage
import com.haddadian.universalremote.atv.proto.RemoteVoiceBegin
import com.haddadian.universalremote.atv.proto.RemoteVoiceEnd
import com.haddadian.universalremote.atv.proto.RemoteVoicePayload
import com.google.protobuf.ByteString
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Android TV Remote v2 voice framing.
 *
 * The protocol uses a persistent session id and PCM payload frames. The current
 * implementation captures 16-bit mono PCM at 8 kHz and keeps individual frames
 * below the documented 20 KiB payload size.
 */
class AndroidTvVoiceEngine(
    private val send: (RemoteMessage) -> Unit
) {
    private val active = AtomicBoolean(false)
    private val nextSessionId = AtomicInteger(1)
    @Volatile private var sessionId = 0

    fun begin(packageName: String = "com.haddadian.universalremote"): Result<Unit> = runCatching {
        check(active.compareAndSet(false, true)) { "Voice session already active" }
        sessionId = nextSessionId.getAndIncrement()
        send(
            RemoteMessage.newBuilder()
                .setRemoteVoiceBegin(
                    RemoteVoiceBegin.newBuilder()
                        .setSessionId(sessionId)
                        .setPackageName(packageName)
                        .build()
                )
                .build()
        )
    }.onFailure { active.set(false) }

    fun sendPcm(pcm: ByteArray): Result<Unit> = runCatching {
        check(active.get()) { "Voice session is not active" }
        require(pcm.isNotEmpty())
        require(pcm.size <= 20 * 1024) { "Voice frame exceeds 20 KiB" }
        send(
            RemoteMessage.newBuilder()
                .setRemoteVoicePayload(
                    RemoteVoicePayload.newBuilder()
                        .setSessionId(sessionId)
                        .setSamples(ByteString.copyFrom(pcm))
                        .build()
                )
                .build()
        )
    }

    fun end(): Result<Unit> = runCatching {
        if (active.compareAndSet(true, false)) {
            send(
                RemoteMessage.newBuilder()
                    .setRemoteVoiceEnd(RemoteVoiceEnd.newBuilder().setSessionId(sessionId).build())
                    .build()
            )
        }
        sessionId = 0
    }
}
