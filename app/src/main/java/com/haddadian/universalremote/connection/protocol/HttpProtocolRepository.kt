package com.haddadian.universalremote.connection.protocol

import com.haddadian.universalremote.BuildConfig

import com.haddadian.universalremote.security.NetworkSecurityPolicy

import com.haddadian.universalremote.connection.ConnectionTransport
import com.haddadian.universalremote.connection.TvFingerprint
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.util.concurrent.TimeUnit

/**
 * HTTPS repository for signed, declarative protocol packages.
 *
 * The index contains compatibility metadata, so a normal connection downloads only:
 *   1) the tiny manifest; and
 *   2) the single package selected for the discovered TV.
 *
 * No DEX/APK/JAR code is downloaded or executed.
 */
class HttpProtocolRepository(
    baseUrl: String,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(false)
        .followSslRedirects(false)
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .build()
) : ProtocolRepository {
    private val base: URI = URI(baseUrl.trimEnd('/') + "/").also {
        NetworkSecurityPolicy.requireHttps(it.toString())
    }

    @Volatile private var cachedIndex: RemoteProtocolIndex? = null
    @Volatile private var indexLoadedAtMs: Long = 0L
    private val indexTtlMs = 6L * 60L * 60L * 1000L

    override suspend fun findCompatible(manufacturer: String?, platform: String?, model: String?): ProtocolPackage? {
        val index = loadIndex() ?: return null
        val fingerprint = TvFingerprint(manufacturer = manufacturer, platform = platform, model = model)
        val stableProfiles = index.releases
            .filter { it.status == ReleaseStatus.STABLE }
            .map { it.toProfileStub() }
        val betaProfiles = index.releases
            .filter { it.status == ReleaseStatus.BETA }
            .map { it.toProfileStub() }
        val match = ProtocolSelector().select(fingerprint, stableProfiles)
            ?: ProtocolSelector().select(fingerprint, betaProfiles)
            ?: return null
        val release = index.releases.firstOrNull {
            it.id == match.profile.manifest.id && it.version == match.profile.manifest.version
        } ?: return null
        return downloadRelease(release)
    }

    override suspend fun get(id: ProtocolId, version: Int?): ProtocolPackage? {
        val index = loadIndex() ?: return null
        val release = index.releases
            .filter { it.id == id && (version == null || it.version == version) }
            .filter { it.status == ReleaseStatus.STABLE || it.status == ReleaseStatus.BETA }
            .maxByOrNull { it.version }
            ?: return null
        return downloadRelease(release)
    }

    private fun downloadRelease(release: RemoteProtocolRelease): ProtocolPackage? {
        val text = getText(release.path) ?: return null
        return runCatching {
            val parsed = ProtocolPackageCodec.decode(text.toByteArray(Charsets.UTF_8))
            require(parsed.manifest.id == release.id)
            require(parsed.manifest.version == release.version)
            require(parsed.manifest.minAppVersion == release.minAppVersion)
            require(parsed.manifest.payloadSha256.equals(release.payloadSha256, true))
            require(sha256Hex(parsed.payloadBytes).equals(release.payloadSha256, true))
            require(release.sizeBytes in 1..(10L * 1024L * 1024L))
            require(parsed.manifest.manufacturer == release.manufacturer)
            require(parsed.manifest.platform == release.platform)
            require(parsed.profile.supportedManufacturers == release.supportedManufacturers)
            require(parsed.manifest.modelPatterns == release.modelPatterns)
            require(parsed.manifest.trust == ProtocolTrust.SIGNED)
            require(parsed.manifest.source == ProtocolSource.REMOTE)
            parsed
        }.getOrNull()
    }

    private fun loadIndex(): RemoteProtocolIndex? {
        val now = System.currentTimeMillis()
        cachedIndex?.takeIf { now - indexLoadedAtMs < indexTtlMs }?.let { return it }
        val text = getText("manifest.json") ?: return cachedIndex
        val parsed = runCatching { RemoteProtocolIndex.decode(text) }.getOrNull() ?: return cachedIndex
        cachedIndex = parsed
        indexLoadedAtMs = now
        return parsed
    }

    private fun getText(relativePath: String): String? {
        val normalizedPath = relativePath.trim()
        if (normalizedPath.isEmpty() || normalizedPath.startsWith("/")) return null
        if (normalizedPath.contains("\\") || normalizedPath.split('/').any { it == ".." }) return null
        val target = runCatching { base.resolve(normalizedPath) }.getOrNull() ?: return null
        if (!target.host.equals(base.host, ignoreCase = true)) return null
        NetworkSecurityPolicy.requireHttps(target.toString())
        val request = Request.Builder().url(target.toString()).get()
            .header("Accept", "application/json")
            .header("User-Agent", "UniversalTVRemote/${BuildConfig.VERSION_NAME} ProtocolClient")
            .build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body ?: return@use null
                if (body.contentLength() > 10L * 1024L * 1024L) return@use null
                body.bytes().takeIf { it.size <= 10 * 1024 * 1024 }
                    ?.toString(Charsets.UTF_8)
            }
        }.getOrNull()
    }
}

private data class RemoteProtocolIndex(val releases: List<RemoteProtocolRelease>) {
    companion object {
        fun decode(text: String): RemoteProtocolIndex {
            val root = JSONObject(text)
            require(root.optInt("schemaVersion", 0) == 1)
            val array = root.optJSONArray("protocols") ?: JSONArray()
            val values = buildList {
                for (i in 0 until array.length()) {
                    add(RemoteProtocolRelease.decode(array.getJSONObject(i)))
                }
            }
            require(values.map { it.id to it.version }.distinct().size == values.size)
            return RemoteProtocolIndex(values)
        }
    }
}

private data class RemoteProtocolRelease(
    val id: ProtocolId,
    val version: Int,
    val status: ReleaseStatus,
    val minAppVersion: Int,
    val manufacturer: String,
    val platform: String,
    val modelPatterns: Set<String>,
    val supportedManufacturers: Set<String>,
    val discoveryHints: Set<String>,
    val transports: Set<ConnectionTransport>,
    val authMode: String,
    val capabilities: Set<String>,
    val payloadSha256: String,
    val sizeBytes: Long,
    val path: String
) {
    companion object {
        fun decode(o: JSONObject): RemoteProtocolRelease {
            val id = ProtocolId(o.getString("id"))
            val version = o.getInt("version").also { require(it >= 1) }
            val status = ReleaseStatus.valueOf(o.getString("status"))
            val transports = stringSet(o, "transports").mapNotNull {
                runCatching { ConnectionTransport.valueOf(it) }.getOrNull()
            }.toSet()
            val hash = o.getString("payloadSha256")
            require(hash.matches(Regex("^[a-fA-F0-9]{64}$")))
            return RemoteProtocolRelease(
                id = id, version = version, status = status,
                minAppVersion = o.getInt("minAppVersion"),
                manufacturer = o.getString("manufacturer"), platform = o.getString("platform"),
                modelPatterns = stringSet(o, "modelPatterns"),
                supportedManufacturers = stringSet(o, "supportedManufacturers"),
                discoveryHints = stringSet(o, "discoveryHints"),
                transports = transports, authMode = o.optString("authMode", "none"),
                capabilities = stringSet(o, "capabilities"), payloadSha256 = hash,
                sizeBytes = o.getLong("sizeBytes"), path = o.getString("path")
            )
        }

        private fun stringSet(o: JSONObject, name: String): Set<String> {
            val a = o.optJSONArray(name) ?: return emptySet()
            return buildSet { for (i in 0 until a.length()) add(a.getString(i)) }
        }
    }

    fun toProfileStub(): ProtocolProfile = ProtocolProfile(
        manifest = ProtocolPackageManifest(
            id = id, manufacturer = manufacturer, platform = platform, version = version,
            minAppVersion = minAppVersion, modelPatterns = modelPatterns,
            payloadSha256 = payloadSha256, signatureBase64 = null,
            trust = ProtocolTrust.SIGNED, source = ProtocolSource.REMOTE
        ),
        supportedManufacturers = supportedManufacturers,
        discoveryHints = discoveryHints,
        transports = transports,
        authMode = authMode,
        capabilities = capabilities
    )
}
