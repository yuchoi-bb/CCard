package com.ccard.tracker.sms

import android.content.Context
import android.provider.Telephony
import com.ccard.tracker.data.Transaction
import com.ccard.tracker.parser.CardSmsParser
import java.time.ZoneId

/**
 * 앱 설치 이전에 이미 수신된 문자함(Inbox)을 스캔해 초기 데이터를 채운다.
 * READ_SMS 권한이 승인된 상태에서만 호출해야 한다.
 */
object SmsImporter {
    fun importExisting(context: Context): List<Transaction> {
        val transactions = mutableListOf<Transaction>()
        val cursor = context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(Telephony.Sms.BODY, Telephony.Sms.DATE),
            null,
            null,
            "${Telephony.Sms.DATE} DESC",
        ) ?: return transactions

        cursor.use {
            val bodyIndex = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIndex = it.getColumnIndexOrThrow(Telephony.Sms.DATE)
            while (it.moveToNext()) {
                val body = it.getString(bodyIndex) ?: continue
                val date = it.getLong(dateIndex)
                val parsed = CardSmsParser.parse(body, date) ?: continue
                transactions += Transaction(
                    cardCompany = parsed.cardCompany,
                    cardLast4 = parsed.cardLast4,
                    amount = parsed.amount,
                    merchantName = parsed.merchantName,
                    transactedAtEpochMillis = parsed.transactedAt
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli(),
                    isCancellation = parsed.isCancellation,
                    isInstallment = parsed.isInstallment,
                    rawSms = body,
                )
            }
        }
        return transactions
    }
}
