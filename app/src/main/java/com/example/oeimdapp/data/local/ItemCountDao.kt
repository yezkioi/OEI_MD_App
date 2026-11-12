package com.example.oeimdapp.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemCountDao {
    // Mengambil semua data hitungan untuk toko dan list tertentu
    @Query("SELECT * FROM item_counts WHERE storeId = :storeId AND listId = :listId")
    fun getCountsForList(storeId: String, listId: String): Flow<List<ItemCountEntity>>

    // Menyimpan atau memperbarui angka yang diketik MD (− / + / ketik manual)
    @Upsert
    suspend fun upsertCount(itemCount: ItemCountEntity)

    // Menghapus hitungan untuk satu list tertentu (Clear list ini)
    @Query("DELETE FROM item_counts WHERE storeId = :storeId AND listId = :listId")
    suspend fun clearListCounts(storeId: String, listId: String)

    // Menghapus semua hitungan untuk toko tersebut (Clear semua list toko ini)
    @Query("DELETE FROM item_counts WHERE storeId = :storeId")
    suspend fun clearStoreCounts(storeId: String)
}