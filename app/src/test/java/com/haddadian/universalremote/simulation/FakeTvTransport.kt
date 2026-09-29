package com.haddadian.universalremote.simulation

import com.haddadian.universalremote.Command
import com.haddadian.universalremote.Credentials
import com.haddadian.universalremote.TvDevice
import com.haddadian.universalremote.TvTransport

/** Deterministic transport for end-to-end command-pipeline tests without hardware. */
class FakeTvTransport : TvTransport {
    val received = mutableListOf<Command>()
    var connectResult: Result<Unit> = Result.success(Unit)
    override suspend fun connect(device: TvDevice, auth: Credentials): Result<Unit> = connectResult
    override suspend fun send(device: TvDevice, command: Command): Result<Unit> { received += command; return Result.success(Unit) }
}
