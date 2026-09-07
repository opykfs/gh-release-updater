package com.example.githubupdater.ui.trackinglist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.githubupdater.R
import com.example.githubupdater.domain.model.UpdateStatus
import com.example.githubupdater.ui.components.AppIcon

/**
 * 追踪列表单项卡片：图标 / 应用名 / 仓库 / 版本 / 状态徽标；
 * 有更新时可点击重新弹出更新对话框；右上角菜单可删除。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingListItem(
    item: TrackedItemUiState,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val track = item.track

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(
                packageName = track.packageName,
                size = 44.dp,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    text = track.appLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Text(
                    text = track.repoFullName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Text(
                    text = stringResource(R.string.version_current, track.localVersionName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                StatusContent(item = item)
            }

            // 更多操作（删除）
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = null,
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_item_delete)) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

/** 根据状态展示徽标/进度。 */
@Composable
private fun StatusContent(item: TrackedItemUiState, modifier: Modifier = Modifier) {
    val progress = item.downloadProgress
    if (progress != null) {
        // 下载中
        Column(modifier = modifier.fillMaxWidth().padding(top = 6.dp)) {
            if (progress > 0f) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Text(
                text = if (progress > 0f) {
                    stringResource(R.string.download_percent, (progress * 100).toInt())
                } else {
                    stringResource(R.string.download_in_progress)
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        return
    }

    when (item.status) {
        UpdateStatus.UP_TO_DATE -> {
            val latest = item.latestTag
            Text(
                text = if (!latest.isNullOrBlank()) {
                    stringResource(R.string.status_up_to_date_with_tag, latest)
                } else {
                    stringResource(R.string.status_up_to_date)
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.Medium,
                modifier = modifier.padding(top = 4.dp),
            )
        }

        UpdateStatus.UPDATE_AVAILABLE -> {
            val latest = item.latestTag.orEmpty()
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = modifier.padding(top = 4.dp),
            ) {
                Text(
                    text = stringResource(R.string.status_update_available, latest),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }

        UpdateStatus.SKIPPED -> {
            val latest = item.latestTag.orEmpty()
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = modifier.padding(top = 4.dp),
            ) {
                Text(
                    text = stringResource(R.string.status_skipped, latest),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }

        UpdateStatus.FAILED -> {
            Column(modifier = modifier.padding(top = 4.dp)) {
                Text(
                    text = stringResource(
                        R.string.status_failed,
                        item.errorMessage ?: stringResource(R.string.error_generic),
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                )
            }
        }

        UpdateStatus.NO_APK -> {
            // 中性提示（非红色错误）：仓库有 Release 但无可下载 APK
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = modifier.padding(top = 4.dp),
            ) {
                Text(
                    text = stringResource(R.string.status_no_apk),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }

        UpdateStatus.CHECKING -> {
            Text(
                text = stringResource(R.string.status_checking),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = modifier.padding(top = 4.dp),
            )
        }

        UpdateStatus.IDLE -> {
            Text(
                text = stringResource(R.string.status_idle),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = modifier.padding(top = 4.dp),
            )
        }
    }
}
