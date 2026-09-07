package com.example.githubupdater.domain.repo

import java.io.File

/**
 * APK 安装接口（FileProvider + ACTION_VIEW 拉起系统安装器）。
 */
interface InstallRepository {
    /** 是否已授权「安装未知来源应用」 */
    fun isInstallAllowed(): Boolean

    /** 打开本应用的「允许安装未知应用」系统设置页（ACTION_MANAGE_UNKNOWN_APP_SOURCES） */
    fun openUnknownSourceSettings()

    /**
     * 拉起系统安装器安装 APK。
     * @throws Exception 当系统没有可处理安装的应用时抛出（message 中文）。
     */
    fun openInstaller(apkFile: File)
}
