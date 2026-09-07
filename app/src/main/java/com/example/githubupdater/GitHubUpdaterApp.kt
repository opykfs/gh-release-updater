package com.example.githubupdater

import android.app.Application
import com.example.githubupdater.di.AppContainer

/**
 * 应用入口：持有依赖容器。
 */
class GitHubUpdaterApp : Application() {

    val container: AppContainer by lazy { AppContainer(this) }
}
