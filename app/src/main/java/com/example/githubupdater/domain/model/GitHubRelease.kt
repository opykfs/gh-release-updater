package com.example.githubupdater.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * GitHub Releases API「latest release」响应 DTO。
 * 仅保留本工具所需字段，多余字段由 Json(ignoreUnknownKeys=true) 忽略。
 */
@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String = "",
    val name: String? = null,
    val body: String? = null,
    @SerialName("published_at") val publishedAt: String? = null,
    val assets: List<ReleaseAsset> = emptyList(),
)
