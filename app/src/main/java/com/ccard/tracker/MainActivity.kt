package com.ccard.tracker

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ccard.tracker.data.CardCondition
import com.ccard.tracker.ui.AddCardConditionDialog
import com.ccard.tracker.ui.MonthlyStatusScreen
import com.ccard.tracker.ui.MonthlyStatusViewModel
import com.ccard.tracker.ui.TransactionListScreen
import com.ccard.tracker.ui.UpdateAvailableDialog
import com.ccard.tracker.update.UpdateManager

class MainActivity : ComponentActivity() {

    private var smsPermissionGranted by mutableStateOf(false)
    private var notificationAccessGranted by mutableStateOf(true)

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        smsPermissionGranted = results.values.all { it }
    }

    private lateinit var updateManager: UpdateManager

    private val requestInstallPermission = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { /* 결과와 무관하게 사용자가 다시 업데이트 버튼을 누르면 downloadAndInstall이 재시도됨 */ }

    override fun onResume() {
        super.onResume()
        notificationAccessGranted =
            NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
    }

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
                    val transactions by viewModel.transactions.collectAsState()
                    val updateInfo by viewModel.updateInfo.collectAsState()
                    var showConditionDialog by remember { mutableStateOf(false) }
                    var editingCondition by remember { mutableStateOf<CardCondition?>(null) }
                    var selectedTab by remember { mutableStateOf(0) }

                    // 최초 설치 후 SMS 권한이 허용되는 즉시 문자함 전체를 한 번 스캔해 기존 카드 문자를 정리한다.
                    LaunchedEffect(smsPermissionGranted) {
                        if (smsPermissionGranted) {
                            viewModel.importOnFirstLaunchIfNeeded()
                        }
                    }

                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text("카드 실적 체크  v${BuildConfig.VERSION_NAME}") },
                                actions = {
                                    IconButton(onClick = { viewModel.importExistingSms() }) {
                                        Icon(Icons.Filled.Refresh, contentDescription = "기존 문자함 스캔")
                                    }
                                },
                            )
                        },
                        floatingActionButton = {
                            FloatingActionButton(onClick = {
                                editingCondition = null
                                showConditionDialog = true
                            }) {
                                Icon(Icons.Filled.Add, contentDescription = "카드 조건 추가")
                            }
                        },
                    ) { padding ->
                        Column(modifier = Modifier.padding(padding)) {
                            if (!notificationAccessGranted) {
                                Card(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            "하나카드처럼 RCS(챗봇)로 오는 승인 메시지는 문자함에 없어 읽지 못합니다. 알림 접근 권한을 켜면 알림에서 수집할 수 있습니다.",
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f),
                                        )
                                        TextButton(onClick = {
                                            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                        }) { Text("설정") }
                                    }
                                }
                            }
                            TabRow(selectedTabIndex = selectedTab) {
                                Tab(
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0 },
                                    text = { Text("실적") },
                                )
                                Tab(
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1 },
                                    text = { Text("문자 내역") },
                                )
                            }
                            when (selectedTab) {
                                0 -> MonthlyStatusScreen(
                                    statuses = statuses,
                                    onEdit = { condition ->
                                        editingCondition = condition
                                        showConditionDialog = true
                                    },
                                    onDelete = viewModel::deleteCondition,
                                    modifier = Modifier.weight(1f),
                                )
                                else -> TransactionListScreen(
                                    transactions = transactions,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }

                    if (showConditionDialog) {
                        AddCardConditionDialog(
                            initial = editingCondition,
                            onDismiss = { showConditionDialog = false },
                            onConfirm = { condition ->
                                viewModel.addCondition(condition)
                                showConditionDialog = false
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
