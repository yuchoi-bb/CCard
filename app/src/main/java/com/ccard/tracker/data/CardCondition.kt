package com.ccard.tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PerformancePeriod {
    /** 이번 달 1일 ~ 말일 실적을 기준으로 판단 */
    CURRENT_MONTH,

    /** 전월 1일 ~ 말일 실적을 기준으로 이번 달 혜택 여부를 판단 (카드사에 흔한 방식) */
    PREV_MONTH,
}

@Entity(tableName = "card_conditions")
data class CardCondition(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** 사용자가 구분하기 위한 별칭 (예: "신한 딥드림") */
    val nickname: String,
    val cardCompany: CardCompany,
    /** 카드가 여러 장일 때 SMS의 카드 뒷 4자리로 구분. 모르면 null. */
    val cardLast4: String?,
    val thresholdAmount: Long,
    val performancePeriod: PerformancePeriod,
    val excludeInstallment: Boolean = false,
    /**
     * 매입일 기준으로 실적을 산정하는 카드(예: 현대카드)를 위한 근사치.
     * 월말 마지막 N일 동안의 승인 건은 전표 매입이 다음 달로 넘어간다고 보고 다음 달 실적으로 계산한다.
     * 0이면 승인일 기준 그대로 계산한다.
     */
    val settlementLagDays: Int = 0,
    /**
     * 실적 제외 가맹점 키워드 (쉼표 구분, 예: "아파트관리비,도시가스,상품권").
     * 가맹점명(없으면 문자 원문)에 키워드가 포함된 거래는 실적 집계에서 제외한다.
     */
    val excludeKeywords: String = "",
    val memo: String = "",
)
