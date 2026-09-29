package com.haddadian.universalremote.data

import com.haddadian.universalremote.domain.TvDevice

data class TvProfile(
    val device: TvDevice,
    val displayName: String = device.model ?: device.manufacturer ?: device.name,
    val room: String? = null
)

interface TvProfileStore {
    suspend fun getAll(): List<TvProfile>
    suspend fun save(profile: TvProfile)
    suspend fun remove(id: String)
}
