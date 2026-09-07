package com.example.githubupdater.ui.addtracking

import com.example.githubupdater.domain.model.InstalledApp

/** 添加追踪页 UI 状态。 */
data class AddTrackingUiState(
    val apps: List<InstalledApp> = emptyList(),
    val query: String = "",
    val selectedPackageName: String? = null,
    val repoInput: String = "",
    val repoError: String? = null,
    val isLoadingApps: Boolean = true,
    val isSaving: Boolean = false,
    /** 已在追踪列表中的包名（用于灰显） */
    val trackedPackageNames: Set<String> = emptySet(),
)
