package com.store.depositrelay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** 재부팅 후 백그라운드 서비스 자동 시작 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            if (Prefs.isConfigured(context.applicationContext)) {
                KeepAliveService.start(context.applicationContext)
            }
        }
    }
}
