package com.example.githubupdater.data.repository

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import com.example.githubupdater.domain.repo.DownloadRepository
import com.example.githubupdater.domain.repo.DownloadState
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 基于系统 DownloadManager 的下载实现。
 * - 目标目录：context.getExternalFilesDir(DIRECTORY_DOWNLOADS)（免存储权限）；
 * - 注册 ACTION_DOWNLOAD_COMPLETE 广播（API33+ 用 RECEIVER_NOT_EXPORTED）并轮询进度。
 */
class DownloadRepositoryImpl(
    private val context: Context,
) : DownloadRepository {

    private companion object {
        const val APK_MIME = "application/vnd.android.package-archive"
        const val POLL_INTERVAL_MS = 1_000L
        const val MAX_RETRY_QUERY = 5
    }

    private val downloadManager: DownloadManager by lazy {
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    }

    /** id → 预期落盘文件（供完成态解析 File）。 */
    private val destinationFiles = ConcurrentHashMap<Long, File>()

    override fun start(url: String, fileName: String): Long {
        val safeName = sanitizeFileName(fileName)
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: return -1L
        if (!dir.exists() && !dir.mkdirs()) return -1L

        val target = File(dir, safeName)
        // 同名旧文件会令 enqueue 返回 ERROR_FILE_ALREADY_EXISTS，先清理以便覆盖
        runCatching { if (target.exists()) target.delete() }

        return try {
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(safeName)
                .setMimeType(APK_MIME)
                .setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )
                .setDestinationInExternalFilesDir(
                    context, Environment.DIRECTORY_DOWNLOADS, safeName
                )
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(false)
            val id = downloadManager.enqueue(request)
            if (id != -1L) {
                destinationFiles[id] = target
            }
            id
        } catch (e: Exception) {
            -1L
        }
    }

    override fun observe(id: Long): Flow<DownloadState> = callbackFlow {
        // 完成广播 + 每秒轮询的混合驱动
        val ticks = Channel<Unit>(Channel.CONFLATED)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE &&
                    intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) == id
                ) {
                    ticks.trySend(Unit)
                }
            }
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.RECEIVER_NOT_EXPORTED
        } else {
            0
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            flags
        )

        val ticker = launch {
            while (isActive) {
                ticks.trySend(Unit)
                delay(POLL_INTERVAL_MS)
            }
        }

        val poller = launch {
            var emptyCount = 0
            while (isActive) {
                ticks.receive()
                val state = queryState(id)
                if (state != null) {
                    trySend(state)
                    if (state is DownloadState.Completed || state is DownloadState.Failed) {
                        break
                    }
                } else {
                    emptyCount++
                    // 连续多次查不到记录视为任务已不存在
                    if (emptyCount >= MAX_RETRY_QUERY) {
                        trySend(DownloadState.Failed("下载任务不存在"))
                        break
                    }
                }
            }
            close()
        }

        awaitClose {
            ticker.cancel()
            poller.cancel()
            runCatching { context.unregisterReceiver(receiver) }
        }
    }.flowOn(Dispatchers.IO)

    private fun queryState(id: Long): DownloadState? {
        return try {
            val query = DownloadManager.Query().setFilterById(id)
            downloadManager.query(query).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val statusIndex = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
                    val status = cursor.getInt(statusIndex)
                    when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            val file = destinationFiles.remove(id)
                                ?: fileFromLocalUri(
                                    cursor.getString(
                                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI)
                                    )
                                )
                            if (file != null && file.exists()) {
                                DownloadState.Completed(file)
                            } else {
                                DownloadState.Failed("下载文件不存在或已被清理")
                            }
                        }

                        DownloadManager.STATUS_FAILED -> {
                            val reasonIndex =
                                cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)
                            DownloadState.Failed(
                                "下载失败（错误码 ${cursor.getInt(reasonIndex)}）"
                            )
                        }

                        else -> {
                            val downloaded = cursor.getLong(
                                cursor.getColumnIndexOrThrow(
                                    DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR
                                )
                            )
                            val total = cursor.getLong(
                                cursor.getColumnIndexOrThrow(
                                    DownloadManager.COLUMN_TOTAL_SIZE_BYTES
                                )
                            )
                            DownloadState.Running(
                                downloadedBytes = downloaded,
                                totalBytes = if (total > 0) total else null,
                            )
                        }
                    }
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            DownloadState.Failed("查询下载状态失败")
        }
    }

    private fun fileFromLocalUri(localUri: String?): File? {
        if (localUri.isNullOrBlank()) return null
        val path = runCatching { Uri.parse(localUri).path }.getOrNull()
        if (path.isNullOrBlank()) return null
        val file = File(path)
        return if (file.exists()) file else null
    }

    private fun sanitizeFileName(raw: String): String {
        val base = raw.substringAfterLast('/').substringAfterLast('\\').trim()
        val cleaned = base
            .replace(Regex("[^A-Za-z0-9._\\-]"), "_")
            .ifBlank { "release" }
        return if (cleaned.endsWith(".apk", ignoreCase = true)) cleaned else "$cleaned.apk"
    }
}
