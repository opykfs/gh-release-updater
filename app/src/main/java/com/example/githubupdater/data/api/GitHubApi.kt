package com.example.githubupdater.data.api

import com.example.githubupdater.domain.model.GitHubRelease
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * GitHub REST API（Retrofit 接口）。
 */
interface GitHubApi {
    /** 获取仓库最新正式 Release（GitHub 已排除 pre-release/draft） */
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun latestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
    ): GitHubRelease
}
