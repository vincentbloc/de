package com.store.depositrelay

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 화면에 보여줄 최근 전달 기록(최대 50건) */
object ForwardLog {
    private const val KEY = "log"
    private const val MAX = 50
    private val fmt = SimpleDateFormat("MM/dd HH:mm:ss", Locale.KOREA)

    @Synchronized
    fun add(c: Context, source: String, text: String, status: String) {
        val arr = JSONArray(Prefs.getString(c, KEY, "[]"))
        val o = JSONObject()
            .put("t", fmt.format(Date()))
            .put("src", source)
            .put("text", text.take(60).replace("\n", " "))
            .put("status", status)
        val out = JSONArray()
        out.put(o)
        for (i in 0 until minOf(arr.length(), MAX - 1)) out.put(arr.get(i))
        Prefs.putString(c, KEY, out.toString())
    }

    fun dump(c: Context): String {
        val arr = JSONArray(Prefs.getString(c, KEY, "[]"))
        if (arr.length() == 0) return "아직 전달된 입금 알림이 없습니다."
        val sb = StringBuilder()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val icon = when (o.getString("status")) {
                "전송됨" -> "✅"; "대기(재전송)" -> "⏳"; "제외" -> "➖"; else -> "❌"
            }
            sb.append(icon).append(" ").append(o.getString("t"))
                .append(" [").append(o.getString("src")).append("] ")
                .append(o.getString("text")).append("\n")
        }
        return sb.toString().trimEnd()
    }
}
