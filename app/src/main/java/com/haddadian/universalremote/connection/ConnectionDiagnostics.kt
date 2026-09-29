package com.haddadian.universalremote.connection

data class DiagnosticEvent(val timestampMs: Long, val stage: ConnectionStage, val message: String)
data class ConnectionDiagnostics(val device: TvFingerprint, val events: List<DiagnosticEvent>) {
    val durationMs: Long get() = if (events.size < 2) 0 else events.last().timestampMs - events.first().timestampMs
}

class DiagnosticRecorder(private val maxEvents: Int = 200) {
    private val events = ArrayDeque<DiagnosticEvent>()
    fun record(stage: ConnectionStage, message: String) {
        if (events.size >= maxEvents) events.removeFirst()
        events.addLast(DiagnosticEvent(System.currentTimeMillis(), stage, message))
    }
    fun snapshot(): List<DiagnosticEvent> = events.toList()
}
