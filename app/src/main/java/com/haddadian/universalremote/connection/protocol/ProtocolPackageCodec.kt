package com.haddadian.universalremote.connection.protocol

import com.haddadian.universalremote.connection.ConnectionTransport
import org.json.JSONArray
import org.json.JSONObject
import java.util.Base64

/** Declarative package format. It contains data/rules only, never DEX/APK code. */
object ProtocolPackageCodec {
    fun encode(value: ProtocolPackage): ByteArray {
        val root = JSONObject()
        root.put("id", value.manifest.id.value)
        root.put("manufacturer", value.manifest.manufacturer)
        root.put("platform", value.manifest.platform)
        root.put("version", value.manifest.version)
        root.put("minAppVersion", value.manifest.minAppVersion)
        root.put("modelPatterns", JSONArray(value.manifest.modelPatterns.toList()))
        root.put("payloadSha256", value.manifest.payloadSha256)
        root.put("signature", value.manifest.signatureBase64 ?: JSONObject.NULL)
        root.put("trust", value.manifest.trust.name)
        root.put("source", value.manifest.source.name)
        root.put("payloadBase64", Base64.getEncoder().encodeToString(value.payloadBytes))
        return root.toString().toByteArray(Charsets.UTF_8)
    }

    fun encodePayload(profile: ProtocolProfile): ByteArray =
        profileJson(profile).toString().toByteArray(Charsets.UTF_8)

    fun decode(bytes: ByteArray): ProtocolPackage {
        val root = JSONObject(bytes.toString(Charsets.UTF_8))
        val id = ProtocolId(root.getString("id"))
        val payload = Base64.getDecoder().decode(root.getString("payloadBase64"))
        val manifest = ProtocolPackageManifest(
            id = id,
            manufacturer = root.getString("manufacturer"),
            platform = root.getString("platform"),
            version = root.getInt("version"),
            minAppVersion = root.getInt("minAppVersion"),
            modelPatterns = root.optStringSet("modelPatterns"),
            payloadSha256 = root.getString("payloadSha256"),
            signatureBase64 = root.optString("signature").takeIf { it.isNotBlank() && it != "null" },
            trust = ProtocolTrust.valueOf(root.getString("trust")),
            source = ProtocolSource.valueOf(root.getString("source"))
        )
        val profile = parseProfile(JSONObject(payload.toString(Charsets.UTF_8)), manifest)
        return ProtocolPackage(manifest, profile, payload)
    }

    private fun profileJson(profile: ProtocolProfile): JSONObject = JSONObject().apply {
        put("supportedManufacturers", JSONArray(profile.supportedManufacturers.toList()))
        put("discoveryHints", JSONArray(profile.discoveryHints.toList()))
        put("transports", JSONArray(profile.transports.map { it.name }))
        put("authMode", profile.authMode)
        put("capabilities", JSONArray(profile.capabilities.toList()))
        put("commands", JSONArray(profile.commands.map {
            JSONObject().put("command", it.command).put("wireValue", it.wireValue)
        }))
    }

    private fun parseProfile(json: JSONObject, manifest: ProtocolPackageManifest): ProtocolProfile {
        val commandsJson = json.optJSONArray("commands") ?: JSONArray()
        val commands = buildList {
            for (i in 0 until commandsJson.length()) {
                val item = commandsJson.getJSONObject(i)
                add(ProtocolCommandDefinition(item.getString("command"), item.getString("wireValue")))
            }
        }
        return ProtocolProfile(
            manifest = manifest,
            discoveryHints = json.optStringSet("discoveryHints"),
            supportedManufacturers = json.optStringSet("supportedManufacturers"),
            transports = json.optStringSet("transports")
                .mapNotNull { runCatching { ConnectionTransport.valueOf(it) }.getOrNull() }
                .toSet(),
            authMode = json.optString("authMode", "none"),
            commands = commands,
            capabilities = json.optStringSet("capabilities")
        )
    }

    private fun JSONObject.optStringSet(name: String): Set<String> {
        val array = optJSONArray(name) ?: return emptySet()
        return buildSet { for (i in 0 until array.length()) add(array.getString(i)) }
    }
}
