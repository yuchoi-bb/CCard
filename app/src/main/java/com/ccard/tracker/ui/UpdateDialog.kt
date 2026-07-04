package com.ccard.tracker.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.ccard.tracker.update.UpdateInfo

@Composable
fun UpdateAvailableDialog(
    info: UpdateInfo,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("새 버전이 있습니다") },
        text = {
            Text("v${info.versionName}\n\n${info.releaseNotes.ifBlank { "업데이트를 진행하시겠습니까?" }}")
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("업데이트") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("나중에") } },
    )
}
