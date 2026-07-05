package com.ccard.tracker.sms

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.ccard.tracker.CCardApplication
import com.ccard.tracker.data.Transaction
import com.ccard.tracker.parser.CardSmsParser
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * RCS(챗봇)·알림톡처럼 SMS 수신함에 저장되지 않는 카드 승인 메시지를 잡기 위한 보조 수집 경로.
 * 모든 앱의 알림 제목+본문을 [CardSmsParser]에 통과시켜, 카드 승인으로 파싱되는 것만 저장한다.
 * (rawSms 유니크 제약이 있어 같은 알림이 반복 게시돼도 중복 저장되지 않는다)
 *
 * 사용하려면 시스템 설정에서 이 앱에 "알림 접근" 권한을 켜야 한다.
 */
class CardNotificationListenerService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val body = listOf(title, if (bigText.length > text.length) bigText else text)
            .filter { it.isNotBlank() }
            .joinToString("\n")
        if (body.isBlank()) return

        val parsed = CardSmsParser.parse(body, sbn.postTime) ?: return

        val db = (applicationContext as CCardApplication).database
        scope.launch {
            db.transactionDao().insert(
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
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
