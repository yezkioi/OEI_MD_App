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

    // Pilihan tema: "system" (Ikuti Sistem), "light" (Terang), "dark" (Gelap)
    var themeMode: String
        get() = prefs.getString("theme_mode", "system") ?: "system"
        set(value) = prefs.edit { putString("theme_mode", value) }

    // ID toko terakhir yang dibuka MD (untuk kartu pintasan di layar awal)
    var lastStoreId: String
        get() = prefs.getString("last_store_id", "") ?: ""
        set(value) = prefs.edit { putString("last_store_id", value) }

    // Cache JSON hasil sinkronisasi terakhir agar aplikasi bisa jalan offline
    var cachedSyncJson: String
        get() = prefs.getString("cached_sync_json", "") ?: ""
        set(value) = prefs.edit { putString("cached_sync_json", value) }

    // Waktu sinkronisasi terakhir (untuk indikator status di layar)
    var lastSyncTime: String
        get() = prefs.getString("last_sync_time", "") ?: ""
        set(value) = prefs.edit { putString("last_sync_time", value) }
}