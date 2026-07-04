package com.ccard.tracker.domain

import com.ccard.tracker.data.CardCondition
import com.ccard.tracker.data.PerformancePeriod
import com.ccard.tracker.data.Transaction
import java.time.LocalDate
import java.time.ZoneId

data class CardStatus(
    val condition: CardCondition,
    val currentAmount: Long,
    val remainingAmount: Long,
    val isSatisfied: Boolean,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
)

object MonthlyPerformanceCalculator {

    fun periodFor(condition: CardCondition, today: LocalDate = LocalDate.now()): Pair<LocalDate, LocalDate> =
        when (condition.performancePeriod) {
            PerformancePeriod.CURRENT_MONTH ->
                today.withDayOfMonth(1) to today.withDayOfMonth(today.lengthOfMonth())
            PerformancePeriod.PREV_MONTH -> {
                val prevMonth = today.minusMonths(1)
                prevMonth.withDayOfMonth(1) to prevMonth.withDayOfMonth(prevMonth.lengthOfMonth())
            }
        }

    fun calculate(
        condition: CardCondition,
        transactions: List<Transaction>,
        today: LocalDate = LocalDate.now(),
    ): CardStatus {
        val (start, end) = periodFor(condition, today)
        val zone = ZoneId.systemDefault()
        val startMillis = start.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

        val relevant = transactions.filter { tx ->
            tx.cardCompany == condition.cardCompany &&
                (condition.cardLast4 == null || tx.cardLast4 == condition.cardLast4) &&
                tx.transactedAtEpochMillis in startMillis..endMillis &&
                (!condition.excludeInstallment || !tx.isInstallment)
        }

        val net = relevant.sumOf { if (it.isCancellation) -it.amount else it.amount }
            .coerceAtLeast(0)

        return CardStatus(
            condition = condition,
            currentAmount = net,
            remainingAmount = (condition.thresholdAmount - net).coerceAtLeast(0),
            isSatisfied = net >= condition.thresholdAmount,
            periodStart = start,
            periodEnd = end,
        )
    }
}
