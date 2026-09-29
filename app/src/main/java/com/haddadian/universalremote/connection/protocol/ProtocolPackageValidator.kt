package com.haddadian.universalremote.connection.protocol

class ProtocolPackageValidator(
    private val appVersion: Int,
    private val maxPackageBytes: Int = 10 * 1024 * 1024,
    private val signatureVerifier: ProtocolSignatureVerifier
) {
    fun validate(packageValue: ProtocolPackage, allowBuiltIn: Boolean = false): ProtocolManagerResult<Unit> {
        val manifest = packageValue.manifest
        if (manifest.trust == ProtocolTrust.BUILTIN) {
            if (!allowBuiltIn || manifest.source != ProtocolSource.BUILTIN)
                return ProtocolManagerResult.Failure(ProtocolManagerError.VerificationFailed())
        } else {
            if (manifest.trust != ProtocolTrust.SIGNED ||
                manifest.source != ProtocolSource.REMOTE && manifest.source != ProtocolSource.INSTALLED)
                return ProtocolManagerResult.Failure(ProtocolManagerError.VerificationFailed())
        }
        if (packageValue.payloadBytes.size > maxPackageBytes)
            return ProtocolManagerResult.Failure(ProtocolManagerError.InvalidPackage("size limit exceeded"))
        if (manifest.minAppVersion > appVersion)
            return ProtocolManagerResult.Failure(ProtocolManagerError.Incompatible("minimum app version is ${manifest.minAppVersion}"))
        if (!manifest.payloadSha256.matches(Regex("^[a-fA-F0-9]{64}$")))
            return ProtocolManagerResult.Failure(ProtocolManagerError.InvalidPackage("invalid SHA-256"))
        if (!sha256Hex(packageValue.payloadBytes).equals(manifest.payloadSha256, true))
            return ProtocolManagerResult.Failure(ProtocolManagerError.VerificationFailed())
        if (manifest.trust == ProtocolTrust.SIGNED && !signatureVerifier.verify(manifest, packageValue.payloadBytes))
            return ProtocolManagerResult.Failure(ProtocolManagerError.VerificationFailed())
        if (manifest.version < 1)
            return ProtocolManagerResult.Failure(ProtocolManagerError.InvalidPackage("invalid version"))
        if (packageValue.profile.manifest.id != manifest.id || packageValue.profile.manifest.version != manifest.version)
            return ProtocolManagerResult.Failure(ProtocolManagerError.InvalidPackage("manifest/profile mismatch"))
        if (packageValue.profile.transports.isEmpty())
            return ProtocolManagerResult.Failure(ProtocolManagerError.InvalidPackage("no transport declared"))

        val declaredCommands = packageValue.profile.commands.map { it.command.trim() }.toSet()
        val capabilityByCommand = ProtocolCommandCapabilities.capabilityByCommand
        val missingCommand = capabilityByCommand.entries.firstOrNull { (command, capability) ->
            capability in packageValue.profile.capabilities && command !in declaredCommands
        }
        if (missingCommand != null)
            return ProtocolManagerResult.Failure(ProtocolManagerError.InvalidPackage("capability ${missingCommand.value} requires command ${missingCommand.key}"))

        val unsupportedCommand = declaredCommands.firstOrNull { command ->
            capabilityByCommand[command]?.let { it !in packageValue.profile.capabilities } == true
        }
        if (unsupportedCommand != null)
            return ProtocolManagerResult.Failure(ProtocolManagerError.InvalidPackage("command $unsupportedCommand is declared without its capability"))
        return ProtocolManagerResult.Success(Unit)
    }
}
