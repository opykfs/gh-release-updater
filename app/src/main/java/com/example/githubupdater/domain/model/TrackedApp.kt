package com.example.githubupdater.domain.model

import kotlinx.serialization.Serializable

/**
 * 一条「本机应用 ↔ GitHub 仓库」追踪记录。
 * packageName 是唯一键。
 */
@Serializable
data class TrackedApp(
    /** 本机应用包名（唯一键） */
    val packageName: String,
    /** GitHub owner */
    val repoOwner: String,
    /** GitHub repo */
    val repoName: String,
    /** 应用显示名 */
    val appLabel: String,
    /** 绑定时的 versionName */
    val localVersionName: String,
    /** 绑定时的 versionCode */
    val localVersionCode: Long = 0,
    /** 加入时间 epoch millis */
    val addedAt: Long = 0,
    /** 最近检测时间 epoch millis（R12） */
    val lastCheckTime: Long? = null,
    /** 最近已知最新 tag（R12） */
    val lastKnownLatestTag: String? = null,
    /** 已跳过版本 tag（R11） */
    val skippedTag: String? = null,
) {
    /** 仓库完整标识 owner/repo */
    val repoFullName: String get() = "$repoOwner/$repoName"
}
