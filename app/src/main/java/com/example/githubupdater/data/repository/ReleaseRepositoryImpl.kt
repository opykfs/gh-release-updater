package com.example.githubupdater.data.repository

import com.example.githubupdater.data.api.GitHubApi
import com.example.githubupdater.domain.model.ReleaseInfo
import com.example.githubupdater.domain.model.TrackedApp
import com.example.githubupdater.domain.repo.ReleaseRepository
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

/**
 * GitHub Releases 检测实现：请求最新正式 Release → 选择首个 .apk asset → 映射领域对象。
 * 异常统一转成中文 message。
 */
class ReleaseRepositoryImpl(
    private val api: GitHubApi,
) : ReleaseRepository {

    private companion object {
        const val MAX_NOTES_LENGTH = 300
        const val MSG_NETWORK = "网络异常，请检查网络"
        const val MSG_NOT_FOUND = "仓库不存在或无 Release"
        const val MSG_RATE_LIMITED = "GitHub 接口限流，请稍后重试"
        const val APK_MIME = "application/vnd.android.package-archive"
    }

    override suspend fun fetchLatest(track: TrackedApp): ReleaseInfo? =
        withContext(Dispatchers.IO) {
            try {
                val release = api.latestRelease(track.repoOwner, track.repoName)
                val asset = release.assets.firstOrNull { it.isUsableApk() }
                    ?: return@withContext null
                ReleaseInfo(
                    tag = release.tagName.ifBlank { track.lastKnownLatestTag ?: "" },
                    title = release.name,
                    notes = release.body?.trim()?.take(MAX_NOTES_LENGTH),
                    downloadUrl = asset.browserDownloadUrl,
                    fileName = asset.name.ifBlank { "${release.tagName}.apk" },
                    publishedAt = release.publishedAt,
                )
            } catch (e: HttpException) {
                throw when (e.code()) {
                    404 -> IllegalStateException(MSG_NOT_FOUND)
                    403 -> IllegalStateException(MSG_RATE_LIMITED)
                    else -> IllegalStateException("网络异常（HTTP ${e.code()}）")
                }
            } catch (e: IOException) {
                throw IllegalStateException(MSG_NETWORK)
            }
        }

    private fun com.example.githubupdater.domain.model.ReleaseAsset.isUsableApk(): Boolean {
        val nameOk = name.endsWith(".apk", ignoreCase = true)
        val mimeOk = contentType == APK_MIME
        return (nameOk || mimeOk) && !browserDownloadUrl.isNullOrBlank()
    }
}
