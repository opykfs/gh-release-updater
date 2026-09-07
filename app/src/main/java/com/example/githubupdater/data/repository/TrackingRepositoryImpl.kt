package com.example.githubupdater.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.githubupdater.domain.model.TrackedApp
import com.example.githubupdater.domain.model.TrackedAppList
import com.example.githubupdater.domain.repo.TrackingRepository
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/** 单进程 DataStore 实例。 */
private val Context.trackingDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "tracking_store"
)

/**
 * 基于 DataStore Preferences 的追踪列表持久化实现。
 * 整个列表编码为一条 JSON 字符串存于 key=tracked_apps。
 */
class TrackingRepositoryImpl(
    private val context: Context,
    private val json: Json,
) : TrackingRepository {

    private companion object {
        val KEY_TRACKED_APPS = stringPreferencesKey("tracked_apps")
    }

    override val tracks: Flow<List<TrackedApp>> =
        context.trackingDataStore.data
            .map { prefs -> decodeList(prefs[KEY_TRACKED_APPS]) }
            .catch { e -> if (e is IOException) emit(emptyList()) else throw e }
            .flowOn(Dispatchers.IO)

    override suspend fun add(track: TrackedApp): Result<Unit> = withContext(Dispatchers.IO) {
        var duplicated = false
        try {
            context.trackingDataStore.edit { prefs ->
                val current = decodeList(prefs[KEY_TRACKED_APPS]).toMutableList()
                if (current.any { it.packageName == track.packageName }) {
                    duplicated = true
                } else {
                    current.add(track)
                    prefs[KEY_TRACKED_APPS] = encodeList(current)
                }
            }
            if (duplicated) {
                Result.failure(IllegalStateException("packageName already tracked: ${track.packageName}"))
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun remove(packageName: String): Unit = withContext(Dispatchers.IO) {
        context.trackingDataStore.edit { prefs ->
            val current = decodeList(prefs[KEY_TRACKED_APPS])
                .filterNot { it.packageName == packageName }
            prefs[KEY_TRACKED_APPS] = encodeList(current)
        }
    }

    override suspend fun update(track: TrackedApp): Unit = withContext(Dispatchers.IO) {
        context.trackingDataStore.edit { prefs ->
            val current = decodeList(prefs[KEY_TRACKED_APPS]).toMutableList()
            val index = current.indexOfFirst { it.packageName == track.packageName }
            if (index >= 0) {
                current[index] = track
            } else {
                current.add(track)
            }
            prefs[KEY_TRACKED_APPS] = encodeList(current)
        }
    }

    private fun decodeList(raw: String?): List<TrackedApp> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString(TrackedAppList.serializer(), raw).items
        }.getOrDefault(emptyList())
    }

    private fun encodeList(items: List<TrackedApp>): String =
        json.encodeToString(TrackedAppList.serializer(), TrackedAppList(items))
}
