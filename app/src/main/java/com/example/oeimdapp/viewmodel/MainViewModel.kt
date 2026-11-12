package com.example.oeimdapp.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.oeimdapp.data.local.AppDatabase
import com.example.oeimdapp.data.prefs.SettingPreferences
import com.example.oeimdapp.data.remote.RetrofitClient
import com.example.oeimdapp.data.repository.OeiRepository
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    // Menyiapkan SharedPreferences dan Database Lokal
    private val prefs = SettingPreferences(application)
    private val database = AppDatabase.getDatabase(application)
    private val repository = OeiRepository(RetrofitClient.apiService, database.itemCountDao())

    // Variabel untuk menyimpan status di layar (Nama MD, URL, Token, dll)
    var mdName by mutableStateOf(prefs.mdName)
        private set

    var webAppUrl by mutableStateOf(prefs.webAppUrl)
        private set

    var appToken by mutableStateOf(prefs.appToken)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    // Fungsi untuk memperbarui Nama MD
    fun updateMdName(newName: String) {
        mdName = newName
        prefs.mdName = newName
    }

    // Fungsi untuk memperbarui URL dan Token Google Sheets
    fun updateSettings(newUrl: String, newToken: String) {
        webAppUrl = newUrl
        appToken = newToken
        prefs.webAppUrl = newUrl
        prefs.appToken = newToken
    }

    // Fungsi untuk melakukan Tes Koneksi (Ping ke Google Sheets)
    fun testConnection(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                // Di sini nanti kita panggil fungsi ping ke server
                isLoading = false
                onResult(true, "Koneksi Berhasil!")
            } catch (e: Exception) {
                isLoading = false
                errorMessage = e.localizedMessage ?: "Terjadi kesalahan koneksi"
                onResult(false, errorMessage!!)
            }
        }
    }
}