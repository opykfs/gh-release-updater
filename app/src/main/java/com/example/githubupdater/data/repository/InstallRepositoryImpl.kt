package com.example.githubupdater.data.repository

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.example.githubupdater.domain.repo.InstallRepository
import java.io.File

/**
 * 安装实现：检查未知来源授权 → 用 FileProvider 暴露 APK → ACTION_VIEW 拉起系统安装器。
 */
class InstallRepositoryImpl(
    private val context: Context,
) : InstallRepository {

    private companion object {
        const val APK_MIME = "application/vnd.android.package-archive"
        const val MSG_NO_ACTIVITY = "未找到可用的安装程序"
    }

    override fun isInstallAllowed(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return context.packageManager.canRequestPackageInstalls()
        }
        return true
    }

    override fun openUnknownSourceSettings() {
        val intent = Intent(
            android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    override fun openInstaller(apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, APK_MIME)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            throw IllegalStateException(MSG_NO_ACTIVITY, e)
        }
    }
}
