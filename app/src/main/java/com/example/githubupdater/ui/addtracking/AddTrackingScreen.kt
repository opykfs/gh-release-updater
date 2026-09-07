package com.example.githubupdater.ui.addtracking

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.githubupdater.R
import com.example.githubupdater.domain.model.InstalledApp
import com.example.githubupdater.ui.components.AppIcon

/**
 * 添加追踪界面：从已装应用中选择 + 填写 GitHub 仓库地址 + 保存。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTrackingScreen(
    onBack: () -> Unit,
    viewModel: AddTrackingViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val visibleApps by viewModel.visibleApps.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.title_add_tracking)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            // 仓库输入
            OutlinedTextField(
                value = uiState.repoInput,
                onValueChange = { viewModel.onRepoInputChange(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = stringResource(R.string.label_repo)) },
                placeholder = { Text(text = stringResource(R.string.repo_placeholder)) },
                isError = !uiState.repoError.isNullOrBlank(),
                supportingText = uiState.repoError?.let { error ->
                    { Text(text = error) }
                },
                singleLine = true,
            )

            Text(
                text = stringResource(R.string.label_select_app),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
            )

            // 搜索
            OutlinedTextField(
                value = uiState.query,
                onValueChange = { viewModel.onQueryChange(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(text = stringResource(R.string.search_apps_hint)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                    )
                },
                singleLine = true,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // 应用列表
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                when {
                    uiState.isLoadingApps -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }

                    uiState.apps.isEmpty() -> {
                        Text(
                            text = stringResource(R.string.empty_apps),
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    visibleApps.isEmpty() -> {
                        Text(
                            text = stringResource(R.string.empty_search_result),
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            items(
                                items = visibleApps,
                                key = { app -> app.packageName },
                            ) { app ->
                                InstalledAppRow(
                                    app = app,
                                    selected = app.packageName == uiState.selectedPackageName,
                                    tracked = app.packageName in uiState.trackedPackageNames,
                                    onClick = { viewModel.onSelectApp(app.packageName) },
                                )
                            }
                        }
                    }
                }
            }

            // 保存按钮
            Button(
                onClick = { viewModel.save(onBack) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                enabled = !uiState.isSaving,
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(text = stringResource(R.string.saving_tracking))
                } else {
                    Text(text = stringResource(R.string.action_save_tracking))
                }
            }
        }
    }
}

/** 单个已安装应用行（单选）。 */
@Composable
private fun InstalledAppRow(
    app: InstalledApp,
    selected: Boolean,
    tracked: Boolean,
    onClick: () -> Unit,
) {
    val contentColor = if (tracked) {
        MaterialTheme.colorScheme.outline
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !tracked, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(
            packageName = app.packageName,
            size = 40.dp,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                text = app.appLabel,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor,
                maxLines = 1,
            )
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Text(
                text = stringResource(R.string.installed_version, app.versionName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (tracked) {
            Text(
                text = stringResource(R.string.tracked_hint),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline,
            )
        } else {
            RadioButton(
                selected = selected,
                onClick = onClick,
            )
        }
    }
}
