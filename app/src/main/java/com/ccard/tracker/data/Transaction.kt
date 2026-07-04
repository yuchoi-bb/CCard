package com.ccard.tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
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
