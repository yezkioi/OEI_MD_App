package com.example.oeimdapp.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // ⚠️ PERHATIAN: Ganti URL di bawah ini dengan URL Web App Google Apps Script Anda yang asli!
    // Contoh: "https://script.google.com/macros/s/AKfycbx.../exec"
    private const val BASE_URL = "https://script.google.com/macros/s/AKfycbwA5YZUSx1eAonPxTTAUikk6DYSiOp04U0KLqFV2GgkhHiZVNp--SPheDCMD23zXIscbw/exec"

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}