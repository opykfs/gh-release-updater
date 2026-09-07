package com.example.githubupdater.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.example.githubupdater.domain.model.InstalledApp
import com.example.githubupdater.domain.repo.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 通过 PackageManager 获取已安装应用列表：
 * 仅保留 launcher 应用、排除本应用、按应用名排序。
 */
class AppRepositoryImpl(
    private val context: Context,
) : AppRepository {

    override suspend fun getInstalledApps(): List<InstalledApp> =
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val selfPackage = context.packageName

            // 主屏幕可启动的应用视为可追踪对象
            val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val launchablePackages = runCatching {
                pm.queryIntentActivities(launcherIntent, 0)
                    .mapNotNull { it.activityInfo?.packageName }
                    .toSet()
            }.getOrDefault(emptySet())

            val installed = runCatching {
                pm.getInstalledPackages(0)
            }.getOrDefault(emptyList())

            installed.asSequence()
                .filter { it.packageName != selfPackage }
                .filter { it.packageName in launchablePackages }
                .mapNotNull { pkg -> toInstalledApp(pm, pkg) }
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.appLabel })
                .toList()
        }

    override suspend fun getInstalledVersionName(packageName: String): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                context.packageManager.getPackageInfo(packageName, 0).versionName
            }.getOrNull()
        }

    private fun toInstalledApp(pm: PackageManager, info: PackageInfo): InstalledApp? {
        val label = runCatching {
            info.applicationInfo?.let { pm.getApplicationLabel(it).toString() }
                ?: info.packageName
        }.getOrDefault(info.packageName)
        return InstalledApp(
            packageName = info.packageName,
            appLabel = label,
            versionName = info.versionName ?: "",
            versionCode = info.longVersionCodeCompat(),
        )
    }

    private fun PackageInfo.longVersionCodeCompat(): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            longVersionCode
        } else {
            @Suppress("DEPRECATION")
            versionCode.toLong()
        }
}
