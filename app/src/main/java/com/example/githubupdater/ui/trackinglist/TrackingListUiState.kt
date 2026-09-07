package com.example.githubupdater.ui.trackinglist

import com.example.githubupdater.domain.model.TrackedApp
import com.example.githubupdater.domain.model.UpdateStatus
import java.io.File

/** 追踪列表页全部 UI 状态。 */
data class TrackingListUiState(
    val items: List<TrackedItemUiState> = emptyList(),
    val isCheckingAll: Boolean = false,
    val activeUpdate: PendingUpdate? = null,
    val activeInstall: PendingInstall? = null,
    val snackbarMessage: String? = null,
)

/** 单个追踪项 UI 状态。 */
data class TrackedItemUiState(
    val track: TrackedApp,
    val status: UpdateStatus = UpdateStatus.IDLE,
    val latestTag: String? = null,
    val releaseNotes: String? = null,
    val downloadUrl: String? = null,
    val fileName: String? = null,
    val errorMessage: String? = null,
    val downloadProgress: Float? = null,
)

/** 「发现新版本」对话框所需数据。 */
data class PendingUpdate(
    val packageName: String,
    val appLabel: String,
    val currentVersion: String,
    val newVersion: String,
    val notes: String?,
    val downloadUrl: String,
    val fileName: String,
)

/** 「下载完成 / 安装确认」对话框所需数据。 */
data class PendingInstall(
    val appLabel: String,
    val versionName: String,
    val apkFile: File,
)
