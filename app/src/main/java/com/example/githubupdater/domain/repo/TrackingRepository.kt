package com.example.githubupdater.domain.repo

import com.example.githubupdater.domain.model.TrackedApp
import kotlinx.coroutines.flow.Flow

/**
 * 追踪项持久化接口（DataStore Preferences JSON 实现）。
 */
interface TrackingRepository {
    /** DataStore 内容变更即发射最新追踪列表 */
    val tracks: Flow<List<TrackedApp>>

    /** 新增追踪项；packageName 已存在则返回 failure */
    suspend fun add(track: TrackedApp): Result<Unit>

    /** 按 packageName 删除追踪项（不存在则静默忽略） */
    suspend fun remove(packageName: String)

    /** 更新追踪项（按 packageName 覆盖；不存在则追加） */
    suspend fun update(track: TrackedApp)
}
