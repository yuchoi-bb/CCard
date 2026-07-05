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
 * 여기 정규식은 실제 수신 문자 샘플을 보며 계속 보정한다.
 *
 * 지원이 확인된 포맷:
 * - 하나카드 라벨형: "승인 / 금액 17,000원 / 카드 하나0*4* / 거래구분 일시불 / 사용처 OOO / 거래시간 07/04 12:21 / 누적금액 1,778,666원"
 */
object CardSmsParser {

    // "신한카드" 같은 회사명 표기 외에, "카드 하나0*4*"처럼 회사명 뒤에 바로
    // 마스킹된 카드번호가 붙는 라벨형 표기도 함께 매칭한다.
    private val companyKeywords: List<Pair<Regex, CardCompany>> = listOf(
        Regex("""신한카드|신한(?=[0-9*(])""") to CardCompany.SHINHAN,
        Regex("""삼성카드|삼성(?=[0-9*(])""") to CardCompany.SAMSUNG,
        Regex("""KB국민카드|국민카드|KB국민(?=[0-9*(])|국민(?=[0-9*(])""") to CardCompany.KB,
        Regex("""현대카드|현대(?=[0-9*(])""") to CardCompany.HYUNDAI,
        Regex("""롯데카드|롯데(?=[0-9*(])""") to CardCompany.LOTTE,
        Regex("""우리카드|우리(?=[0-9*(])""") to CardCompany.WOORI,
        Regex("""NH농협카드|농협카드|NH농협(?=[0-9*(])|농협(?=[0-9*(])""") to CardCompany.NH,
        Regex("""하나카드|하나(?=[0-9*(])""") to CardCompany.HANA,
        Regex("""BC카드|비씨카드""") to CardCompany.BC,
    )

    private val approvalKeywordRegex = Regex("승인")
    private val cancellationRegex = Regex("취소")
    private val installmentRegex = Regex("""\d+개월|할부""")
    private val lumpSumRegex = Regex("일시불")

    private val cumulativeAmountRegex = Regex("""누적(?:금액)?\s*(\d{1,3}(?:,\d{3})*)\s*원""")
    private val amountRegex = Regex("""(\d{1,3}(?:,\d{3})*)\s*원""")
    private val dateTimeRegex = Regex("""(\d{2})/(\d{2})\s+(\d{2}):(\d{2})""")
    private val last4Regex = Regex("""\((\d{4})\)""")

    // 하나카드처럼 "사용처 OOO" / 타사 "가맹점 OOO" 라벨이 있으면 그 값을 가맹점명으로 쓴다.
    private val merchantLabelRegex = Regex("""(?:사용처|가맹점명?)\s*[:：]?\s*(\S.*)""")

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
            runCatching {
                LocalDateTime.of(now.year, month.toInt(), day.toInt(), hour.toInt(), minute.toInt())
            }.getOrNull()
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
        // 1순위: "사용처"/"가맹점" 라벨이 붙은 값
        merchantLabelRegex.find(body)?.let { return it.groupValues[1].trim() }

        // 2순위: 금액·노이즈 키워드가 없는 마지막 줄 (라벨 없는 구형 포맷용 휴리스틱)
        val noiseKeywords = listOf("승인", "누적", "일시불", "할부", "카드", "잔액", "취소", "거래", "손님", "발신", "이용내역")
        return body.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .lastOrNull { line -> noiseKeywords.none { keyword -> line.contains(keyword) } && !amountRegex.containsMatchIn(line) }
    }
}
