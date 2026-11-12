package com.example.oeimdapp.data.remote

import com.example.oeimdapp.data.model.ApiResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    // Fungsi untuk tes koneksi / ping
    @GET(".")
    suspend fun ping(
        @Query("token") token: String,
        @Query("action") action: String = "ping"
    ): Map<String, Any>

    // Fungsi utama untuk menarik seluruh data sinkronisasi dari Google Sheets
    @GET(".")
    suspend fun syncData(
        @Query("token") token: String,
        @Query("action") action: String = "sync",
        @Query("nocache") noCache: Int? = null
    ): ApiResponse
}