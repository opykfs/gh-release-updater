package com.example.githubupdater.domain.repo

import java.io.File
import kotlinx.coroutines.flow.Flow

/**
 * APK 下载接口（基于系统 DownloadManager）。
 */
interface DownloadRepository {
    /**
     * 开始下载；目标目录为应用专属 external-files-dir/Download/。
     * @return DownloadManager 下载 id；失败返回 -1。
     */
    fun start(url: String, fileName: String): Long

    /**
     * 观察下载进度直到完成/失败。
     */
    fun observe(id: Long): Flow<DownloadState>
}

/** 下载状态。 */
sealed interface DownloadState {
    /** 下载中 */
    data class Running(val downloadedBytes: Long, val totalBytes: Long?) : DownloadState

    /** 下载完成，文件已落盘 */
    data class Completed(val file: File) : DownloadState

    /** 下载失败，reason 为原因 */
    data class Failed(val reason: String) : DownloadState
}
