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
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ccard.tracker.data.CardCondition
import com.ccard.tracker.domain.CardStatus
import java.text.NumberFormat
import java.util.Locale

@Composable
fun MonthlyStatusScreen(
    statuses: List<CardStatus>,
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
            CardStatusRow(status, onDelete)
        }
    }
}

@Composable
private fun CardStatusRow(status: CardStatus, onDelete: (CardCondition) -> Unit) {
    val formatter = remember(status) { NumberFormat.getNumberInstance(Locale.KOREA) }
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
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
                IconButton(onClick = { onDelete(status.condition) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "조건 삭제")
                }
            }
            Text("${status.periodStart} ~ ${status.periodEnd}")
            val progress = if (status.condition.thresholdAmount > 0) {
                (status.currentAmount.toFloat() / status.condition.thresholdAmount.toFloat()).coerceIn(0f, 1f)
            } else {
                1f
            }
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
            Text("${formatter.format(status.currentAmount)}원 / ${formatter.format(status.condition.thresholdAmount)}원")
            if (status.isSatisfied) {
                Text("조건 충족", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            } else {
                Text("남은 금액: ${formatter.format(status.remainingAmount)}원")
            }
        }
    }
}
