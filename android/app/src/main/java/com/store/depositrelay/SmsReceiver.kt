package com.store.depositrelay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

/** 문자(SMS) 수신 → 입금 문자만 서버로 전달 */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val c = context.applicationContext
        if (!Prefs.smsEnabled(c)) return
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        val body = StringBuilder()
        var from = ""
        for (m in msgs) {
            body.append(m.messageBody ?: "")
            m.originatingAddress?.let { from = it }
        }
        val text = body.toString()
        if (text.isBlank()) return

        if (DepositFilter.looksLikeDeposit(text)) {
            Forwarder.enqueue(c, "sms", text, smsFrom = from)
        } else {
            // 개인 문자는 서버로 보내지 않으며, 원문도 저장하지 않음
            ForwardLog.add(c, "sms", "(입금 문자가 아니어서 제외)", "제외")
        }
    }
}
