package com.ccard.tracker.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

/** [initial]이 null이면 추가 모드, 값이 있으면 해당 조건을 수정하는 모드로 동작한다. */
@Composable
fun AddCardConditionDialog(
    initial: CardCondition?,
    onDismiss: () -> Unit,
    onConfirm: (CardCondition) -> Unit,
) {
    var nickname by remember { mutableStateOf(initial?.nickname ?: "") }
    var selectedCompany by remember { mutableStateOf(initial?.cardCompany ?: CardCompany.SHINHAN) }
    var thresholdText by remember { mutableStateOf(initial?.thresholdAmount?.toString() ?: "300000") }
    var excludeInstallment by remember { mutableStateOf(initial?.excludeInstallment ?: false) }
    var lagDaysText by remember { mutableStateOf((initial?.settlementLagDays ?: 0).toString()) }
    var excludeKeywordsText by remember { mutableStateOf(initial?.excludeKeywords ?: "") }
    var showThresholdError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "카드 이용조건 추가" else "카드 이용조건 수정") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("카드 별칭 (선택, 예: 신한 딥드림)") },
                    supportingText = { Text("비워두면 카드사 이름으로 표시됩니다") },
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
                    onValueChange = {
                        thresholdText = it.filter(Char::isDigit)
                        showThresholdError = false
                    },
                    label = { Text("월 실적 조건 금액 (원)") },
                    isError = showThresholdError,
                    supportingText = { if (showThresholdError) Text("1원 이상의 금액을 입력하세요") },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
                Row(modifier = Modifier.padding(top = 12.dp)) {
                    Checkbox(checked = excludeInstallment, onCheckedChange = { excludeInstallment = it })
                    Text("할부 결제는 실적에서 제외")
                }
                OutlinedTextField(
                    value = lagDaysText,
                    onValueChange = { lagDaysText = it.filter(Char::isDigit) },
                    label = { Text("월말 이월 일수 (매입 기준 카드용, 보통 0)") },
                    supportingText = { Text("현대카드처럼 매입일 기준이면 2~3 권장. 월말 N일 사용분을 다음 달 실적으로 계산") },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
                OutlinedTextField(
                    value = excludeKeywordsText,
                    onValueChange = { excludeKeywordsText = it },
                    label = { Text("실적 제외 키워드 (쉼표 구분)") },
                    supportingText = { Text("예: 아파트관리비,도시가스,상품권") },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val threshold = thresholdText.toLongOrNull() ?: 0
                if (threshold <= 0) {
                    showThresholdError = true
                } else {
                    onConfirm(
                        CardCondition(
                            id = initial?.id ?: 0,
                            nickname = nickname.trim().ifBlank { selectedCompany.displayName },
                            cardCompany = selectedCompany,
                            cardLast4 = initial?.cardLast4,
                            thresholdAmount = threshold,
                            excludeInstallment = excludeInstallment,
                            settlementLagDays = lagDaysText.toIntOrNull() ?: 0,
                            excludeKeywords = excludeKeywordsText.trim(),
                        ),
                    )
                }
            }) { Text(if (initial == null) "추가" else "저장") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        },
    )
}
