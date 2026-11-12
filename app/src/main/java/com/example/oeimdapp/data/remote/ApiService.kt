package com.example.oeimdapp.data.remote

import com.example.oeimdapp.data.model.ApiResponse
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface ApiService {
    // Fungsi untuk tes koneksi / ping menggunakan URL dari Pengaturan
    @GET
    suspend fun ping(
        @Url url: String,
        @Query("token") token: String,
        @Query("action") action: String = "ping"
    ): Map<String, Any>

    // Fungsi utama untuk menarik seluruh data sinkronisasi dari Google Sheets
    @GET
    suspend fun syncData(
        @Url url: String,
        @Query("token") token: String,
        @Query("action") action: String = "sync",
        @Query("nocache") noCache: Int? = null
    ): ApiResponse
}