package com.example.githubupdater.domain.repo

import com.example.githubupdater.domain.model.ReleaseInfo
import com.example.githubupdater.domain.model.TrackedApp

/**
 * GitHub Releases 检测接口。
 */
interface ReleaseRepository {
    /**
     * 拉取给定仓库的 latest release 并映射为领域对象。
     *
     * @return 返回 null 表示仓库没有可用 .apk release（请求本身成功）。
     * @throws Exception 网络/HTTP 异常，message 为中文（网络异常 / 仓库不存在或无 Release / 限流）。
     */
    suspend fun fetchLatest(track: TrackedApp): ReleaseInfo?
}
