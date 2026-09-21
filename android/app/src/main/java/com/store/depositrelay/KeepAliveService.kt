package com.store.depositrelay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat

/**
 * 백그라운드 유지용 포그라운드 서비스.
 * - 1분마다 재전송 큐를 비우고 서버에 연결 신호(ping)를 보낸다.
 * - 상단에 "입금 알림 전달 중" 상태 알림을 표시한다.
 */
class KeepAliveService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            Forwarder.flush(applicationContext)
            Forwarder.ping(applicationContext)
            handler.postDelayed(this, 60_000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        startAsForeground()
        handler.post(tick)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?) = null

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        super.onDestroy()
    }

    private fun startAsForeground() {
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL, "입금 알림 전달", NotificationManager.IMPORTANCE_MIN)
            ch.description = "매장 서버로 입금 알림을 전달하는 백그라운드 상태"
            mgr.createNotificationChannel(ch)
        }
        val notif: Notification = Notification.Builder(this).let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) it.setChannelId(CHANNEL)
            it.setContentTitle("입금 알림 전달 중")
                .setContentText("입금 문자·알림을 매장 서버로 보내는 중입니다.")
                .setSmallIcon(R.drawable.ic_launcher)
                .setOngoing(true)
                .build()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIF_ID, notif)
        }
    }

    companion object {
        private const val CHANNEL = "relay_status"
        private const val NOTIF_ID = 1001

        fun start(c: Context) {
            ContextCompat.startForegroundService(c, Intent(c, KeepAliveService::class.java))
        }
    }
}
