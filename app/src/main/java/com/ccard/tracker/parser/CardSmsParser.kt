package com.ccard.tracker.parser

import com.ccard.tracker.data.CardCompany
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

data class ParsedCardSms(
    val cardCompany: CardCompany,
    val cardLast4: String?,
    val amount: Long,
    val merchantName: String?,
    val transactedAt: LocalDateTime,
    val isCancellation: Boolean,
    val isInstallment: Boolean,
)

/**
 * 카드사 승인 문자는 카드사/통신사 정책에 따라 표기가 자주 바뀐다.
 * 여기 정규식은 흔히 알려진 포맷 기준 추정치이므로, 실제 수신 문자 샘플을 보고
 * companyKeywords / amountRegex / dateTimeRegex를 계속 보정해야 정확도가 올라간다.
 */
object CardSmsParser {

    private val companyKeywords: List<Pair<Regex, CardCompany>> = listOf(
        Regex("신한카드") to CardCompany.SHINHAN,
        Regex("삼성카드") to CardCompany.SAMSUNG,
        Regex("KB국민카드|국민카드") to CardCompany.KB,
        Regex("현대카드") to CardCompany.HYUNDAI,
        Regex("롯데카드") to CardCompany.LOTTE,
        Regex("우리카드") to CardCompany.WOORI,
        Regex("NH농협카드|농협카드") to CardCompany.NH,
        Regex("하나카드") to CardCompany.HANA,
        Regex("BC카드") to CardCompany.BC,
    )

    private val approvalKeywordRegex = Regex("승인")
    private val cancellationRegex = Regex("취소")
    private val installmentRegex = Regex("""\d+개월|할부""")
    private val lumpSumRegex = Regex("일시불")

    private val cumulativeAmountRegex = Regex("""누적\s*(\d{1,3}(?:,\d{3})*)\s*원""")
    private val amountRegex = Regex("""(\d{1,3}(?:,\d{3})*)\s*원""")
    private val dateTimeRegex = Regex("""(\d{2})/(\d{2})\s+(\d{2}):(\d{2})""")
    private val last4Regex = Regex("""\((\d{4})\)""")

    fun parse(body: String, receivedAtEpochMillis: Long): ParsedCardSms? {
        val normalized = body.replace("[Web발신]", "").trim()
        if (!approvalKeywordRegex.containsMatchIn(normalized)) return null

        val company = companyKeywords.firstOrNull { (regex, _) -> regex.containsMatchIn(normalized) }
            ?.second ?: return null

        val cumulativeAmount = cumulativeAmountRegex.find(normalized)?.groupValues?.get(1)?.replace(",", "")
        val amount = amountRegex.findAll(normalized)
            .map { it.groupValues[1].replace(",", "") }
            .firstOrNull { it != cumulativeAmount }
            ?.toLongOrNull() ?: return null

        val last4 = last4Regex.find(normalized)?.groupValues?.get(1)

        val transactedAt = dateTimeRegex.find(normalized)?.let { match ->
            val (month, day, hour, minute) = match.destructured
            val now = LocalDateTime.now(ZoneId.systemDefault())
            LocalDateTime.of(now.year, month.toInt(), day.toInt(), hour.toInt(), minute.toInt())
        } ?: LocalDateTime.ofInstant(Instant.ofEpochMilli(receivedAtEpochMillis), ZoneId.systemDefault())

        return ParsedCardSms(
            cardCompany = company,
            cardLast4 = last4,
            amount = amount,
            merchantName = extractMerchant(normalized),
            transactedAt = transactedAt,
            isCancellation = cancellationRegex.containsMatchIn(normalized),
            isInstallment = installmentRegex.containsMatchIn(normalized) && !lumpSumRegex.containsMatchIn(normalized),
        )
    }

    private fun extractMerchant(body: String): String? {
        val noiseKeywords = listOf("승인", "누적", "일시불", "할부", "카드", "잔액", "취소")
        return body.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .lastOrNull { line -> noiseKeywords.none { keyword -> line.contains(keyword) } && !amountRegex.containsMatchIn(line) }
    }
}
