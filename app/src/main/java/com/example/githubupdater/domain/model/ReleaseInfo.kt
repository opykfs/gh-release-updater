package com.example.githubupdater.domain.model

/**
 * 单次检测得到的「最新版本」领域对象。
 * 由 GitHubRelease + 首个 .apk asset 映射而来。
 */
data class ReleaseInfo(
    /** Release tag（如 v2.1.0） */
    val tag: String,
    val title: String?,
    /** Release 说明摘要（body 前 ~300 字符） */
    val notes: String?,
    /** 首个 .apk asset 的下载地址 */
    val downloadUrl: String?,
    /** asset.name（或 tag.apk） */
    val fileName: String?,
    val publishedAt: String?,
)
