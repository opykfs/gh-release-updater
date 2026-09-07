package com.example.githubupdater.ui.trackinglist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.githubupdater.R

/**
 * 「发现新版本」对话框：展示当前/最新版本与更新说明，
 * 提供 暂不 / 下载更新 / 跳过此版本。只有点「下载更新」才发起下载。
 */
@Composable
fun UpdateAvailableDialog(
    update: PendingUpdate,
    onDismiss: () -> Unit,
    onDownload: (PendingUpdate) -> Unit,
    onSkipVersion: (PendingUpdate) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.SystemUpdate,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = { Text(text = stringResource(R.string.dialog_update_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = update.appLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.dialog_update_current, update.currentVersion),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    text = stringResource(R.string.dialog_update_latest, update.newVersion),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (!update.notes.isNullOrBlank()) {
                    Text(
                        text = stringResource(R.string.dialog_update_notes_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    Text(
                        text = update.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onDownload(update) }) {
                Text(text = stringResource(R.string.action_download_update))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onSkipVersion(update) }) {
                    Text(text = stringResource(R.string.action_skip_version))
                }
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.action_later))
                }
            }
        },
    )
}
