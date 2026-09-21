package com.store.depositrelay

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * 입금 알림을 서버로 전송한다.
 * - 오프라인/실패 시 큐에 저장해 두었다가 자동 재전송한다.
 */
object Forwarder {
    private const val QUEUE = "queue"
    private val io = Executors.newSingleThreadExecutor()

    /** 입금 알림 1건을 큐에 넣고 전송 시도 */
    fun enqueue(
        c: Context, source: String, rawText: String,
        pkg: String? = null, smsFrom: String? = null
    ) {
        val item = JSONObject()
            .put("device_id", Prefs.deviceId(c))
            .put("device_name", Prefs.deviceName(c))
            .put("source", source)
            .put("raw_text", rawText)
            .put("sent_at", System.currentTimeMillis())
        if (pkg != null) item.put("package", pkg)
        if (smsFrom != null) item.put("sms_from", smsFrom)

        synchronized(this) {
            val q = JSONArray(Prefs.getString(c, QUEUE, "[]"))
            q.put(item)
            Prefs.putString(c, QUEUE, q.toString())
        }
        ForwardLog.add(c, source, rawText, "대기(재전송)")
        flush(c)
    }

    /** 큐에 쌓인 항목을 순서대로 전송 (성공한 것만 제거) */
    fun flush(c: Context) {
        io.execute {
            synchronized(this) {
                if (!Prefs.isConfigured(c)) return@execute
                val url = Prefs.serverUrl(c)
                val token = Prefs.token(c)
                var q = JSONArray(Prefs.getString(c, QUEUE, "[]"))
                val remain = JSONArray()
                var allOk = true
                for (i in 0 until q.length()) {
                    val item = q.getJSONObject(i)
                    if (allOk) {
                        val ok = post(url, token, item.toString())
                        if (ok) {
                            ForwardLog.add(c, item.optString("source"),
                                item.optString("raw_text"), "전송됨")
                        } else {
                            allOk = false
                            remain.put(item) // 실패 → 다음 기회에
                        }
                    } else {
                        remain.put(item)
                    }
                }
                Prefs.putString(c, QUEUE, remain.toString())
            }
        }
    }

    /** 서버에 연결 확인용 신호(heartbeat) */
    fun ping(c: Context, onResult: ((Boolean, String) -> Unit)? = null) {
        io.execute {
            if (!Prefs.isConfigured(c)) { onResult?.invoke(false, "설정 미완료"); return@execute }
            val body = JSONObject()
                .put("device_id", Prefs.deviceId(c))
                .put("device_name", Prefs.deviceName(c))
                .put("source", "ping")
                .put("raw_text", "")
                .toString()
            val ok = post(Prefs.serverUrl(c), Prefs.token(c), body)
            onResult?.invoke(ok, if (ok) "서버 연결 정상" else "서버 연결 실패")
        }
    }

    /** 실제 HTTP POST. 2xx 이면 true */
    private fun post(urlStr: String, token: String, jsonBody: String): Boolean {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10000
                readTimeout = 15000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Authorization", "Bearer $token")
            }
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(jsonBody) }
            val code = conn.responseCode
            // 응답 본문 소비(연결 재사용)
            try {
                (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()?.use(BufferedReader::readText)
            } catch (_: Exception) {}
            code in 200..299
        } catch (e: Exception) {
            false
        } finally {
            conn?.disconnect()
        }
    }
}
