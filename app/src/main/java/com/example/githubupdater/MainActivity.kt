package com.example.githubupdater

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.githubupdater.ui.navigation.AppNavHost
import com.example.githubupdater.ui.theme.GitHubUpdaterTheme

/**
 * 单 Activity 入口。
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as GitHubUpdaterApp).container
        setContent {
            GitHubUpdaterTheme {
                AppNavHost(container = container)
            }
        }
    }
}
