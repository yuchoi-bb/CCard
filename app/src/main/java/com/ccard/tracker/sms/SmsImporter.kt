package com.ccard.tracker.sms

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import com.ccard.tracker.data.Transaction
import com.ccard.tracker.parser.CardSmsParser
import com.ccard.tracker.parser.ParsedCardSms
import java.time.ZoneId

/**
 * 앱 설치 이전에 이미 수신된 문자함을 스캔해 초기 데이터를 채운다.
 * SMS(Inbox)뿐 아니라 MMS 본문도 함께 읽는다 — 카드 승인 카드형 메시지("확인된 발신번호")가
 * MMS로 저장되는 경우가 있기 때문이다.
 * READ_SMS 권한이 승인된 상태에서만 호출해야 한다.
 */
object SmsImporter {
    fun importExisting(context: Context): List<Transaction> =
        importSms(context) + importMms(context)

    private fun ParsedCardSms.toTransaction(rawBody: String) = Transaction(
        cardCompany = cardCompany,
        cardLast4 = cardLast4,
        amount = amount,
        merchantName = merchantName,
        transactedAtEpochMillis = transactedAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        isCancellation = isCancellation,
        isInstallment = isInstallment,
        rawSms = rawBody,
    )

    private fun importSms(context: Context): List<Transaction> {
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
                transactions += parsed.toTransaction(body)
            }
        }
        return transactions
    }

    private fun importMms(context: Context): List<Transaction> {
        val transactions = mutableListOf<Transaction>()
        val cursor = context.contentResolver.query(
            Telephony.Mms.Inbox.CONTENT_URI,
            arrayOf(Telephony.Mms._ID, Telephony.Mms.DATE),
            null,
            null,
            "${Telephony.Mms.DATE} DESC",
        ) ?: return transactions

        cursor.use {
            val idIndex = it.getColumnIndexOrThrow(Telephony.Mms._ID)
            // MMS의 DATE는 초 단위라 밀리초로 변환한다.
            val dateIndex = it.getColumnIndexOrThrow(Telephony.Mms.DATE)
            while (it.moveToNext()) {
                val mmsId = it.getString(idIndex) ?: continue
                val dateMillis = it.getLong(dateIndex) * 1000
                val body = readMmsText(context, mmsId) ?: continue
                val parsed = CardSmsParser.parse(body, dateMillis) ?: continue
                transactions += parsed.toTransaction(body)
            }
        }
        return transactions
    }

    private fun readMmsText(context: Context, mmsId: String): String? {
        val partUri = Uri.parse("content://mms/part")
        val cursor = context.contentResolver.query(
            partUri,
            arrayOf("_id", "ct", "text"),
            "mid = ?",
            arrayOf(mmsId),
            null,
        ) ?: return null

        val builder = StringBuilder()
        cursor.use {
            val ctIndex = it.getColumnIndexOrThrow("ct")
            val textIndex = it.getColumnIndexOrThrow("text")
            while (it.moveToNext()) {
                if (it.getString(ctIndex) == "text/plain") {
                    it.getString(textIndex)?.let { text ->
                        if (builder.isNotEmpty()) builder.append("\n")
                        builder.append(text)
                    }
                }
            }
        }
        return builder.toString().ifBlank { null }
    }
}
