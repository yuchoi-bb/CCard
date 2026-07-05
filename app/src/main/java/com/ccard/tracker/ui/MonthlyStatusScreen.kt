package com.ccard.tracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ccard.tracker.data.CardCondition
import com.ccard.tracker.data.Transaction
import com.ccard.tracker.domain.CardStatus
import com.ccard.tracker.domain.MonthPerformance
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MonthlyStatusScreen(
    statuses: List<CardStatus>,
    onEdit: (CardCondition) -> Unit,
    onDelete: (CardCondition) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (statuses.isEmpty()) {
        Column(
            modifier = modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text("등록된 카드 조건이 없습니다. 우측 하단 + 버튼으로 카드 이용조건을 추가하세요.")
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(statuses, key = { it.condition.id }) { status ->
            CardStatusRow(status, onEdit, onDelete)
        }
    }
}

@Composable
private fun CardStatusRow(
    status: CardStatus,
    onEdit: (CardCondition) -> Unit,
    onDelete: (CardCondition) -> Unit,
) {
    val formatter = remember { NumberFormat.getNumberInstance(Locale.KOREA) }
    var expanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${status.condition.nickname} (${status.condition.cardCompany.displayName})",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onEdit(status.condition) }) {
                    Icon(Icons.Filled.Edit, contentDescription = "조건 수정")
                }
                IconButton(onClick = { onDelete(status.condition) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "조건 삭제")
                }
            }

            MonthPerformanceSection(
                label = "이번 달",
                performance = status.thisMonth,
                thresholdAmount = status.condition.thresholdAmount,
                formatter = formatter,
            )
            MonthPerformanceSection(
                label = "지난달",
                performance = status.lastMonth,
                thresholdAmount = status.condition.thresholdAmount,
                formatter = formatter,
            )

            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "상세 닫기" else "상세보기")
            }
            if (expanded) {
                TransactionDetailSection("이번 달 관련 문자", status.thisMonth.transactions, formatter)
                TransactionDetailSection("지난달 관련 문자", status.lastMonth.transactions, formatter)
            }
        }
    }
}

@Composable
private fun MonthPerformanceSection(
    label: String,
    performance: MonthPerformance,
    thresholdAmount: Long,
    formatter: NumberFormat,
) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("${performance.periodStart} ~ ${performance.periodEnd}", style = MaterialTheme.typography.bodySmall)
        }
        val progress = if (thresholdAmount > 0) {
            (performance.amount.toFloat() / thresholdAmount.toFloat()).coerceIn(0f, 1f)
        } else {
            1f
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                "${formatter.format(performance.amount)}원 / ${formatter.format(thresholdAmount)}원",
                modifier = Modifier.weight(1f),
            )
            if (performance.isSatisfied) {
                Text("충족", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            } else {
                Text("남은 ${formatter.format(performance.remainingAmount)}원", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun TransactionDetailSection(
    title: String,
    transactions: List<Transaction>,
    formatter: NumberFormat,
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MM/dd HH:mm") }
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    Text(title, fontWeight = FontWeight.SemiBold)
    if (transactions.isEmpty()) {
        Text("해당 기간 문자가 없습니다", style = MaterialTheme.typography.bodySmall)
        return
    }
    transactions.forEach { tx ->
        val time = Instant.ofEpochMilli(tx.transactedAtEpochMillis).atZone(ZoneId.systemDefault())
        val tags = buildList {
            if (tx.isCancellation) add("취소")
            if (tx.isInstallment) add("할부")
        }
        val suffix = if (tags.isNotEmpty()) " (${tags.joinToString(", ")})" else ""
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
            Text(
                "${dateFormatter.format(time)}  ${tx.merchantName ?: "가맹점 미확인"}",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "${formatter.format(tx.amount)}원$suffix",
                style = MaterialTheme.typography.bodySmall,
                color = if (tx.isCancellation) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
