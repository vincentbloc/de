package com.store.depositrelay

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var etUrl: EditText
    private lateinit var etToken: EditText
    private lateinit var etName: EditText
    private lateinit var cbSms: CheckBox
    private lateinit var cbNoti: CheckBox
    private lateinit var cbAll: CheckBox
    private lateinit var tvStatus: TextView
    private lateinit var tvLog: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etUrl = findViewById(R.id.etUrl)
        etToken = findViewById(R.id.etToken)
        etName = findViewById(R.id.etName)
        cbSms = findViewById(R.id.cbSms)
        cbNoti = findViewById(R.id.cbNoti)
        cbAll = findViewById(R.id.cbAll)
        tvStatus = findViewById(R.id.tvStatus)
        tvLog = findViewById(R.id.tvLog)

        // 저장된 설정 불러오기
        etUrl.setText(Prefs.serverUrl(this))
        etToken.setText(Prefs.token(this))
        etName.setText(Prefs.deviceName(this))
        cbSms.isChecked = Prefs.smsEnabled(this)
        cbNoti.isChecked = Prefs.notiEnabled(this)
        cbAll.isChecked = Prefs.forwardAll(this)

        findViewById<Button>(R.id.btnSave).setOnClickListener { save() }
        findViewById<Button>(R.id.btnTest).setOnClickListener { testConnection() }
        findViewById<Button>(R.id.btnSms).setOnClickListener { requestSmsPermission() }
        findViewById<Button>(R.id.btnNoti).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        findViewById<Button>(R.id.btnBattery).setOnClickListener { requestIgnoreBattery() }
        findViewById<Button>(R.id.btnRefresh).setOnClickListener { refreshLog() }

        refreshLog()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
        refreshLog()
    }

    private fun save() {
        val url = etUrl.text.toString().trim()
        if (!url.startsWith("http")) {
            toast("서버 주소는 http…/api/ingest.php 형태여야 합니다."); return
        }
        Prefs.save(this, url, etToken.text.toString(), etName.text.toString(),
            cbSms.isChecked, cbNoti.isChecked, cbAll.isChecked)
        requestRuntimePermissions()
        KeepAliveService.start(this)
        toast("저장되었습니다.")
        updateStatus()
    }

    private fun testConnection() {
        if (!Prefs.isConfigured(this)) { toast("먼저 주소·토큰을 저장하세요."); return }
        Forwarder.ping(this) { ok, msg ->
            runOnUiThread { toast(msg) }
        }
    }

    private fun requestSmsPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECEIVE_SMS), 10)
    }

    private fun requestRuntimePermissions() {
        val need = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
            != PackageManager.PERMISSION_GRANTED) need.add(Manifest.permission.RECEIVE_SMS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) need.add(Manifest.permission.POST_NOTIFICATIONS)
        if (need.isNotEmpty()) ActivityCompat.requestPermissions(this, need.toTypedArray(), 11)
    }

    private fun requestIgnoreBattery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                startActivity(Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName")))
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }
    }

    private fun updateStatus() {
        val sb = StringBuilder()
        sb.append(if (Prefs.isConfigured(this)) "● 서버 설정 완료\n" else "○ 서버 설정 필요\n")
        val smsOk = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) ==
            PackageManager.PERMISSION_GRANTED
        sb.append(if (smsOk) "● 문자 수신 권한 있음\n" else "○ 문자 수신 권한 없음\n")
        sb.append(if (isNotiAccessGranted()) "● 알림 접근 허용됨" else "○ 알림 접근 필요(토스·카카오 등)")
        tvStatus.text = sb.toString()
    }

    private fun isNotiAccessGranted(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: ""
        return flat.contains(packageName)
    }

    private fun refreshLog() {
        tvLog.text = ForwardLog.dump(this)
    }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        updateStatus()
    }
}
