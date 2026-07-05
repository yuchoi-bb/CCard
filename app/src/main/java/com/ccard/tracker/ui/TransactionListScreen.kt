package com.ccard.tracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ccard.tracker.data.Transaction
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** SMS에서 인식된 카드 거래를 카드사별로 묶어 보여준다. 파서 정확도를 눈으로 확인하는 용도로도 쓴다. */
@Composable
fun TransactionListScreen(transactions: List<Transaction>, modifier: Modifier = Modifier) {
    if (transactions.isEmpty()) {
        Column(
            modifier = modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text("인식된 카드 문자가 없습니다. 새로고침 버튼으로 문자함을 스캔해보세요.")
        }
        return
    }

    val amountFormatter = remember(transactions) { NumberFormat.getNumberInstance(Locale.KOREA) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MM/dd HH:mm") }
    val grouped = remember(transactions) {
        transactions.sortedByDescending { it.transactedAtEpochMillis }.groupBy { it.cardCompany }
    }

    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        grouped.forEach { (company, groupTransactions) ->
            item(key = "header_${company.name}") {
                val net = groupTransactions.sumOf { if (it.isCancellation) -it.amount else it.amount }
                Text(
                    text = "${company.displayName} · ${groupTransactions.size}건 · ${amountFormatter.format(net)}원",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                )
            }
            items(groupTransactions, key = { it.id }) { tx ->
                TransactionRow(tx, amountFormatter, dateFormatter)
            }
        }
    }
}

@Composable
private fun TransactionRow(
    tx: Transaction,
    amountFormatter: NumberFormat,
    dateFormatter: DateTimeFormatter,
) {
    // 탭하면 문자 원문을 펼쳐서 파싱 결과(가맹점/금액/날짜)가 맞는지 확인할 수 있게 한다.
    var showRaw by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { showRaw = !showRaw },
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            val time = Instant.ofEpochMilli(tx.transactedAtEpochMillis).atZone(ZoneId.systemDefault())
            Text("${dateFormatter.format(time)}  ${tx.merchantName ?: "가맹점 미확인"}")
            val tags = buildList {
                if (tx.isCancellation) add("취소")
                if (tx.isInstallment) add("할부")
            }
            val suffix = if (tags.isNotEmpty()) " (${tags.joinToString(", ")})" else ""
            Text(
                text = "${amountFormatter.format(tx.amount)}원$suffix",
                color = if (tx.isCancellation) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            if (showRaw) {
                Text(
                    text = tx.rawSms,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
