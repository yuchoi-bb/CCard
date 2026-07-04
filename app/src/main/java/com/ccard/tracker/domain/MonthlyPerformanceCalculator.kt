package com.ccard.tracker.domain

import com.ccard.tracker.data.CardCondition
import com.ccard.tracker.data.PerformancePeriod
import com.ccard.tracker.data.Transaction
import java.time.Instant
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

    /**
     * 매입일 기준 카드 근사: 월말 마지막 [lagDays]일 동안 승인된 건은
     * 전표 매입이 다음 달로 넘어간다고 보고 다음 달 1일 실적으로 취급한다.
     */
    private fun effectiveDate(transactedAtEpochMillis: Long, lagDays: Int, zone: ZoneId): LocalDate {
        val date = Instant.ofEpochMilli(transactedAtEpochMillis).atZone(zone).toLocalDate()
        if (lagDays <= 0) return date
        return if (date.dayOfMonth > date.lengthOfMonth() - lagDays) {
            date.plusMonths(1).withDayOfMonth(1)
        } else {
            date
        }
    }

    fun calculate(
        condition: CardCondition,
        transactions: List<Transaction>,
        today: LocalDate = LocalDate.now(),
    ): CardStatus {
        val (start, end) = periodFor(condition, today)
        val zone = ZoneId.systemDefault()
        val excludeKeywords = condition.excludeKeywords
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val relevant = transactions.filter { tx ->
            val effective = effectiveDate(tx.transactedAtEpochMillis, condition.settlementLagDays, zone)
            tx.cardCompany == condition.cardCompany &&
                (condition.cardLast4 == null || tx.cardLast4 == condition.cardLast4) &&
                !effective.isBefore(start) && !effective.isAfter(end) &&
                (!condition.excludeInstallment || !tx.isInstallment) &&
                excludeKeywords.none { keyword -> (tx.merchantName ?: tx.rawSms).contains(keyword) }
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
