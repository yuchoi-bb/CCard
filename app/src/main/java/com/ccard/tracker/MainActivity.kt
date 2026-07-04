package com.ccard.tracker

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ccard.tracker.ui.AddCardConditionDialog
import com.ccard.tracker.ui.MonthlyStatusScreen
import com.ccard.tracker.ui.MonthlyStatusViewModel
import com.ccard.tracker.ui.UpdateAvailableDialog
import com.ccard.tracker.update.UpdateManager

class MainActivity : ComponentActivity() {

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { /* 사용자가 거부하면 SMS 자동 수집이 동작하지 않는다는 안내만 노출하면 충분 */ }

    private lateinit var updateManager: UpdateManager

    private val requestInstallPermission = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { /* 결과와 무관하게 사용자가 다시 업데이트 버튼을 누르면 downloadAndInstall이 재시도됨 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        updateManager = UpdateManager(applicationContext)

        requestPermissions.launch(
            arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS),
        )

        setContent {
            MaterialTheme {
                Surface {
                    val viewModel: MonthlyStatusViewModel = viewModel()
                    val statuses by viewModel.statuses.collectAsState()
                    val updateInfo by viewModel.updateInfo.collectAsState()
                    var showAddDialog by remember { mutableStateOf(false) }

                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text("이번 달 카드 실적") },
                                actions = {
                                    IconButton(onClick = { viewModel.importExistingSms() }) {
                                        Icon(Icons.Filled.Refresh, contentDescription = "기존 문자함 스캔")
                                    }
                                },
                            )
                        },
                        floatingActionButton = {
                            FloatingActionButton(onClick = { showAddDialog = true }) {
                                Icon(Icons.Filled.Add, contentDescription = "카드 조건 추가")
                            }
                        },
                    ) { padding ->
                        MonthlyStatusScreen(statuses = statuses, modifier = Modifier.padding(padding))
                    }

                    if (showAddDialog) {
                        AddCardConditionDialog(
                            onDismiss = { showAddDialog = false },
                            onConfirm = { condition ->
                                viewModel.addCondition(condition)
                                showAddDialog = false
                            },
                        )
                    }

                    updateInfo?.let { info ->
                        UpdateAvailableDialog(
                            info = info,
                            onConfirm = {
                                if (updateManager.hasInstallPermission()) {
                                    updateManager.downloadAndInstall(info)
                                    viewModel.dismissUpdate()
                                } else {
                                    requestInstallPermission.launch(updateManager.installPermissionSettingsIntent())
                                }
                            },
                            onDismiss = { viewModel.dismissUpdate() },
                        )
                    }
                }
            }
        }
    }
}
