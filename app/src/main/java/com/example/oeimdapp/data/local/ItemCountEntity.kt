package com.example.oeimdapp.data.local

import androidx.room.Entity

@Entity(tableName = "item_counts", primaryKeys = ["storeId", "listId", "kode"])
data class ItemCountEntity(
    val storeId: String,   // ID Toko (contoh: BZS)
    val listId: String,    // ID List (contoh: BZS-MP)
    val kode: String,      // Kode barang (contoh: 015)
    val count: Int         // Jumlah yang diisi MD di lapangan
)