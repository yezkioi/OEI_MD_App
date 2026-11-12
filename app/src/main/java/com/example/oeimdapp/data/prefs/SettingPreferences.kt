package com.example.oeimdapp.data.prefs

import android.content.Context
import androidx.core.content.edit

class SettingPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("oei_settings", Context.MODE_PRIVATE)

    var mdName: String
        get() = prefs.getString("md_name", "Budi") ?: "Budi"
        set(value) = prefs.edit { putString("md_name", value) }

    var webAppUrl: String
        get() = prefs.getString("web_url", "") ?: ""
        set(value) = prefs.edit { putString("web_url", value) }

    var appToken: String
        get() = prefs.getString("app_token", "") ?: ""
        set(value) = prefs.edit { putString("app_token", value) }

    // Counter untuk ID Laporan otomatis (misal: LP0042)
    var reportCounter: Int
        get() = prefs.getInt("report_counter", 1)
        set(value) = prefs.edit { putInt("report_counter", value) }
}