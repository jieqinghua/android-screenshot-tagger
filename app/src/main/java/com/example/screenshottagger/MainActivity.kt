package com.example.screenshottagger

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.Switch
import android.widget.TextView

/** 原生 View 设置页：不加载 AndroidX/Compose，关掉后不给常驻进程留负担。 */
class MainActivity : Activity() {
    private lateinit var a11yStatus: TextView
    private lateinit var filesStatus: TextView
    private lateinit var batteryStatus: TextView
    private lateinit var logText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        a11yStatus = findViewById(R.id.a11y_status)
        filesStatus = findViewById(R.id.files_status)
        batteryStatus = findViewById(R.id.battery_status)
        logText = findViewById(R.id.log_text)

        findViewById<Button>(R.id.a11y_button).setOnClickListener {
            // 有 WRITE_SECURE_SETTINGS 时直接开启，否则跳系统设置
            if (A11yState.tryEnable(this)) refresh() else open(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        findViewById<Button>(R.id.files_button).setOnClickListener {
            open(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, packageUri()))
        }
        findViewById<Button>(R.id.battery_button).setOnClickListener {
            open(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri()))
        }
        findViewById<Switch>(R.id.enabled_switch).apply {
            isChecked = Prefs.isEnabled(this@MainActivity)
            setOnCheckedChangeListener { _, checked -> Prefs.setEnabled(this@MainActivity, checked) }
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        a11yStatus.setText(if (A11yState.isEnabled(this)) R.string.a11y_on else R.string.a11y_off)
        filesStatus.setText(if (Environment.isExternalStorageManager()) R.string.files_on else R.string.files_off)
        val ignoring = getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)
        batteryStatus.setText(if (ignoring) R.string.battery_on else R.string.battery_off)
        val log = RenameLog.read(this)
        logText.text = if (log.isEmpty()) getString(R.string.log_empty) else log.joinToString("\n")
    }

    private fun packageUri() = Uri.parse("package:$packageName")

    private fun open(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri()))
        }
    }
}
