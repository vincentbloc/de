package com.store.depositrelay

import android.content.Context
import java.util.UUID

/** 앱 설정 저장소 (SharedPreferences 래퍼) */
object Prefs {
    private const val FILE = "deposit_relay_prefs"

    private fun sp(c: Context) = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun serverUrl(c: Context): String =
        sp(c).getString("server_url", "") ?: ""

    fun token(c: Context): String =
        sp(c).getString("token", "") ?: ""

    fun deviceName(c: Context): String =
        sp(c).getString("device_name", "매장 카운터폰") ?: "매장 카운터폰"

    /** 기기 고유 ID (최초 1회 생성) */
    fun deviceId(c: Context): String {
        val cur = sp(c).getString("device_id", null)
        if (cur != null) return cur
        val id = "and-" + UUID.randomUUID().toString().substring(0, 12)
        sp(c).edit().putString("device_id", id).apply()
        return id
    }

    fun smsEnabled(c: Context): Boolean = sp(c).getBoolean("sms_enabled", true)
    fun notiEnabled(c: Context): Boolean = sp(c).getBoolean("noti_enabled", true)
    /** 입금성 문구 필터 없이 지정 앱의 모든 알림 전달(권장하지 않음) */
    fun forwardAll(c: Context): Boolean = sp(c).getBoolean("forward_all", false)

    fun save(
        c: Context, url: String, token: String, name: String,
        sms: Boolean, noti: Boolean, forwardAll: Boolean
    ) {
        sp(c).edit()
            .putString("server_url", url.trim())
            .putString("token", token.trim())
            .putString("device_name", name.trim().ifEmpty { "매장 카운터폰" })
            .putBoolean("sms_enabled", sms)
            .putBoolean("noti_enabled", noti)
            .putBoolean("forward_all", forwardAll)
            .apply()
    }

    fun isConfigured(c: Context): Boolean =
        serverUrl(c).startsWith("http") && token(c).isNotEmpty()

    // 재전송 큐 / 로그용 저장
    fun getString(c: Context, k: String, d: String): String = sp(c).getString(k, d) ?: d
    fun putString(c: Context, k: String, v: String) = sp(c).edit().putString(k, v).apply()
}
