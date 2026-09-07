package com.example.githubupdater.domain.repo

import com.example.githubupdater.domain.model.InstalledApp

/**
 * 已安装应用查询接口。
 */
interface AppRepository {
    /**
     * 获取本机已安装应用中可作为追踪目标的（launcher 应用、按 label 排序、排除本应用）。
     * 需在主线程外调用（实现内部切 Dispatchers.IO）。
     */
    suspend fun getInstalledApps(): List<InstalledApp>

    /**
     * 读取指定已安装应用当前的 versionName。
     * 应用已卸载/不可见时返回 null（调用方沿用存储的旧值）。
     * 需在主线程外调用（实现内部切 Dispatchers.IO）。
     */
    suspend fun getInstalledVersionName(packageName: String): String?
}
