package com.example.oeimdapp.data.repository

import com.example.oeimdapp.data.local.ItemCountDao
import com.example.oeimdapp.data.local.ItemCountEntity
import com.example.oeimdapp.data.model.ApiResponse
import com.example.oeimdapp.data.remote.ApiService
import kotlinx.coroutines.flow.Flow

class OeiRepository(
    private val apiService: ApiService,
    private val itemCountDao: ItemCountDao
) {
    // Fungsi untuk menarik data terbaru dari Google Sheets via Apps Script
    suspend fun syncFromServer(token: String, noCache: Int? = null): ApiResponse {
        return apiService.syncData(token = token, noCache = noCache)
    }

    // Mengambil data hitungan stok dari database lokal (Room)
    fun getCountsForList(storeId: String, listId: String): Flow<List<ItemCountEntity>> {
        return itemCountDao.getCountsForList(storeId, listId)
    }

    // Menyimpan atau memperbarui angka yang diisi MD (− / + / ketik manual)
    suspend fun updateCount(storeId: String, listId: String, kode: String, count: Int) {
        val entity = ItemCountEntity(
            storeId = storeId,
            listId = listId,
            kode = kode,
            count = count
        )
        itemCountDao.upsertCount(entity)
    }

    // Menghapus hitungan pada list tertentu (Clear list ini)
    suspend fun clearListCounts(storeId: String, listId: String) {
        itemCountDao.clearListCounts(storeId, listId)
    }

    // Menghapus seluruh hitungan pada toko tersebut (Clear semua list toko ini)
    suspend fun clearStoreCounts(storeId: String) {
        itemCountDao.clearStoreCounts(storeId)
    }
}