package com.ccard.tracker.domain

import com.ccard.tracker.data.CardCondition
import com.ccard.tracker.data.Transaction
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** 한 달치 실적: 기간, 누적액, 충족 여부와 함께 집계에 포함된 거래 목록(상세보기용)을 담는다. */
data class MonthPerformance(
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val amount: Long,
    val remainingAmount: Long,
    val isSatisfied: Boolean,
    val transactions: List<Transaction>,
)

data class CardStatus(
    val condition: CardCondition,
    val thisMonth: MonthPerformance,
    val lastMonth: MonthPerformance,
)

object MonthlyPerformanceCalculator {

    fun calculate(
        condition: CardCondition,
        transactions: List<Transaction>,
        today: LocalDate = LocalDate.now(),
    ): CardStatus = CardStatus(
        condition = condition,
        thisMonth = performanceForMonth(condition, transactions, today.withDayOfMonth(1)),
        lastMonth = performanceForMonth(condition, transactions, today.minusMonths(1).withDayOfMonth(1)),
    )

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

    private fun performanceForMonth(
        condition: CardCondition,
        transactions: List<Transaction>,
        monthStart: LocalDate,
    ): MonthPerformance {
        val start = monthStart.withDayOfMonth(1)
        val end = start.withDayOfMonth(start.lengthOfMonth())
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
        }.sortedByDescending { it.transactedAtEpochMillis }

        val net = relevant.sumOf { if (it.isCancellation) -it.amount else it.amount }
            .coerceAtLeast(0)

        return MonthPerformance(
            periodStart = start,
            periodEnd = end,
            amount = net,
            remainingAmount = (condition.thresholdAmount - net).coerceAtLeast(0),
            isSatisfied = net >= condition.thresholdAmount,
            transactions = relevant,
        )
    }
}
