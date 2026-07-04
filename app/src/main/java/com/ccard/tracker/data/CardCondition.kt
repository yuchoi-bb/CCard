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
    val memo: String = "",
)
