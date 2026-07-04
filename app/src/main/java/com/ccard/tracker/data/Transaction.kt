package com.ccard.tracker.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    // 문자함 스캔은 여러 번 반복될 수 있으므로(첫 설치 시 자동 실행 + 수동 새로고침) 같은 원문 문자가
    // 중복 저장되지 않도록 rawSms에 유니크 제약을 둔다.
    indices = [Index(value = ["rawSms"], unique = true)],
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardCompany: CardCompany,
    val cardLast4: String?,
    val amount: Long,
    val merchantName: String?,
    val transactedAtEpochMillis: Long,
    val isCancellation: Boolean,
    val isInstallment: Boolean,
    val rawSms: String,
)
