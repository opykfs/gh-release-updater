package com.example.githubupdater.domain.model

import kotlinx.serialization.Serializable

/** DataStore 持久化包装：整个追踪列表存为一条 JSON。 */
@Serializable
data class TrackedAppList(
    val items: List<TrackedApp> = emptyList(),
)
