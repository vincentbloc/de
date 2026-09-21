package com.store.depositrelay

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * 앱 알림 접근 서비스.
 * 토스·카카오뱅크처럼 문자 없이 앱 알림만 오는 은행의 입금 알림을 잡아 서버로 전달한다.
 * (입금성 문구가 있는 알림만 전달하며, 그 외 개인 알림은 무시한다.)
 */
class DepositNotificationListener : NotificationListenerService() {

    // 같은 알림 중복 전송 완화용(최근 전송 해시/시각)
    private var lastHash = 0
    private var lastTime = 0L

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val c = applicationContext
        if (!Prefs.notiEnabled(c)) return

        val pkg = sbn.packageName
        val ex = sbn.notification.extras ?: return
        val title = ex.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = ex.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val big = ex.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()

        val combined = listOf(title, big.ifEmpty { text })
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .trim()
        if (combined.isBlank()) return

        if (!DepositFilter.looksLikeDeposit(combined)) return

        // 지정 은행앱이거나, 사용자가 "모든 앱 전달"을 켰을 때만
        val allow = DepositFilter.isBankPackage(pkg) || Prefs.forwardAll(c)
        if (!allow) return

        // 짧은 시간 내 동일 알림 중복 방지
        val h = (pkg + "|" + combined).hashCode()
        val now = System.currentTimeMillis()
        if (h == lastHash && now - lastTime < 60_000) return
        lastHash = h; lastTime = now

        Forwarder.enqueue(c, "notification", combined, pkg = pkg)
    }
}
