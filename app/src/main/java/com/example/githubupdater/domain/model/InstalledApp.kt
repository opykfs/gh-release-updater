package com.example.githubupdater.domain.model

/** 本机已安装应用的展示模型（供「添加追踪」界面选择）。 */
data class InstalledApp(
    val packageName: String,
    val appLabel: String,
    val versionName: String,
    val versionCode: Long,
)
