package com.ccard.tracker.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ccard.tracker.data.CardCompany
import com.ccard.tracker.data.CardCondition
import com.ccard.tracker.data.PerformancePeriod

@Composable
fun AddCardConditionDialog(
    onDismiss: () -> Unit,
    onConfirm: (CardCondition) -> Unit,
) {
    var nickname by remember { mutableStateOf("") }
    var selectedCompany by remember { mutableStateOf(CardCompany.SHINHAN) }
    var thresholdText by remember { mutableStateOf("300000") }
    var period by remember { mutableStateOf(PerformancePeriod.CURRENT_MONTH) }
    var excludeInstallment by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("카드 이용조건 추가") },
        text = {
            Column {
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("카드 별칭 (예: 신한 딥드림)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("카드사", modifier = Modifier.padding(top = 12.dp))
                LazyRow {
                    items(CardCompany.entries.filter { it != CardCompany.UNKNOWN }) { company ->
                        FilterChip(
                            selected = company == selectedCompany,
                            onClick = { selectedCompany = company },
                            label = { Text(company.displayName) },
                            modifier = Modifier.padding(end = 4.dp),
                        )
                    }
                }
                OutlinedTextField(
                    value = thresholdText,
                    onValueChange = { thresholdText = it.filter(Char::isDigit) },
                    label = { Text("월 실적 조건 금액 (원)") },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
                Row(modifier = Modifier.padding(top = 12.dp)) {
                    FilterChip(
                        selected = period == PerformancePeriod.CURRENT_MONTH,
                        onClick = { period = PerformancePeriod.CURRENT_MONTH },
                        label = { Text("이번 달 실적") },
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    FilterChip(
                        selected = period == PerformancePeriod.PREV_MONTH,
                        onClick = { period = PerformancePeriod.PREV_MONTH },
                        label = { Text("전월 실적") },
                    )
                }
                Row(modifier = Modifier.padding(top = 12.dp)) {
                    Checkbox(checked = excludeInstallment, onCheckedChange = { excludeInstallment = it })
                    Text("할부 결제는 실적에서 제외")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val threshold = thresholdText.toLongOrNull() ?: 0
                if (nickname.isNotBlank() && threshold > 0) {
                    onConfirm(
                        CardCondition(
                            nickname = nickname,
                            cardCompany = selectedCompany,
                            cardLast4 = null,
                            thresholdAmount = threshold,
                            performancePeriod = period,
                            excludeInstallment = excludeInstallment,
                        ),
                    )
                }
            }) { Text("추가") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        },
    )
}
