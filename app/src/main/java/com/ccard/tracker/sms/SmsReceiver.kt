package com.ccard.tracker.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.ccard.tracker.CCardApplication
import com.ccard.tracker.data.Transaction
import com.ccard.tracker.parser.CardSmsParser
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        val body = messages.joinToString(separator = "") { it.messageBody ?: "" }
        val receivedAt = messages.first().timestampMillis

        val parsed = CardSmsParser.parse(body, receivedAt) ?: return

        val pendingResult = goAsync()
        val app = context.applicationContext as CCardApplication
        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.database.transactionDao().insert(
                    Transaction(
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
                    ),
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
