package com.example.githubupdater.di

import android.content.Context
import com.example.githubupdater.data.api.ApiFactory
import com.example.githubupdater.data.repository.AppRepositoryImpl
import com.example.githubupdater.data.repository.DownloadRepositoryImpl
import com.example.githubupdater.data.repository.InstallRepositoryImpl
import com.example.githubupdater.data.repository.ReleaseRepositoryImpl
import com.example.githubupdater.data.repository.TrackingRepositoryImpl
import com.example.githubupdater.domain.repo.AppRepository
import com.example.githubupdater.domain.repo.DownloadRepository
import com.example.githubupdater.domain.repo.InstallRepository
import com.example.githubupdater.domain.repo.ReleaseRepository
import com.example.githubupdater.domain.repo.TrackingRepository
import kotlinx.serialization.json.Json

/**
 * 极简依赖容器：懒加载各 Repository 单例并注入 Application Context。
 */
class AppContainer(context: Context) {

    /** Application Context（字符串资源、系统服务等） */
    val appContext: Context = context.applicationContext

    private val json: Json by lazy { ApiFactory.json }

    val trackingRepository: TrackingRepository by lazy {
        TrackingRepositoryImpl(appContext, json)
    }

    val appRepository: AppRepository by lazy {
        AppRepositoryImpl(appContext)
    }

    val releaseRepository: ReleaseRepository by lazy {
        ReleaseRepositoryImpl(ApiFactory.githubApi)
    }

    val downloadRepository: DownloadRepository by lazy {
        DownloadRepositoryImpl(appContext)
    }

    val installRepository: InstallRepository by lazy {
        InstallRepositoryImpl(appContext)
    }
}
