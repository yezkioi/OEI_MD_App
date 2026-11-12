package com.example.oeimdapp.data.model // Sesuaikan dengan nama package Anda jika berbeda

import com.google.gson.annotations.SerializedName

data class ApiResponse(
    val ok: Boolean,
    val schema: Int,
    @SerializedName("generated_at") val generatedAt: String,
    @SerializedName("data_hash") val dataHash: String,
    val jenis: List<JenisProdukDto>,
    val areas: List<String>,
    val stores: List<StoreDto>,
    val lists: Map<String, ListDetailDto>,
    val warnings: List<String>?
)

data class JenisProdukDto(
    val nama: String,
    val alias: String,
    @SerializedName("pisah_orientasi") val pisahOrientasi: Boolean,
    val urutan: Int
)

data class StoreDto(
    val id: String,
    val nama: String,
    val area: String,
    @SerializedName("list_ids") val listIds: List<String>
)

data class ListDetailDto(
    val id: String,
    val nama: String,
    val tipe: String, // "Standar" atau "Carousel"
    @SerializedName("jenis_alias") val jenisAlias: String,
    @SerializedName("jenis_nama") val jenisNama: String,
    @SerializedName("jenis_urutan") val jenisUrutan: Int,
    @SerializedName("pisah_orientasi") val pisahOrientasi: Boolean,
    val hash: String,
    val items: List<ItemDto>
)

data class ItemDto(
    val kode: String,
    val label: String,
    val orientasi: String?, // "Vertical" atau "Horizontal"
    val target: Int,
    @SerializedName("best_seller") val bestSeller: Boolean,
    val posisi: Int?,
    @SerializedName("file_id") val fileId: String,
    val urut: Int
)