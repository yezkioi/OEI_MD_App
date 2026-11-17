package com.example.oeimdapp.data.repository

import com.example.oeimdapp.data.local.ItemCountDao
import com.example.oeimdapp.data.local.ItemCountEntity
import com.example.oeimdapp.data.local.UndoRedoDao
import com.example.oeimdapp.data.local.UndoRedoEntity
import com.example.oeimdapp.data.model.ApiResponse
import com.example.oeimdapp.data.remote.ApiService
import com.example.oeimdapp.data.remote.RetrofitClient
import kotlinx.coroutines.flow.Flow

class OeiRepository(
    private val apiService: ApiService,
    private val itemCountDao: ItemCountDao,
    private val undoRedoDao: UndoRedoDao
) {
    // Menarik data terbaru dari Google Sheets menggunakan URL & Token dari Pengaturan
    suspend fun syncFromServer(webAppUrl: String, token: String, noCache: Int? = null): ApiResponse {
        val cleanUrl = RetrofitClient.cleanWebAppUrl(webAppUrl)
        return apiService.syncData(url = cleanUrl, token = token.trim(), noCache = noCache)
    }

    // Mengambil data hitungan stok untuk satu list
    fun getCountsForList(storeId: String, listId: String): Flow<List<ItemCountEntity>> {
        return itemCountDao.getCountsForList(storeId, listId)
    }

    // Mengambil data hitungan stok untuk seluruh list pada satu toko
    fun getCountsForStore(storeId: String): Flow<List<ItemCountEntity>> {
        return itemCountDao.getCountsForStore(storeId)
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

    // Memulihkan data hitungan setelah Clear dibatalkan (Urungkan)
    suspend fun restoreCounts(items: List<ItemCountEntity>) {
        if (items.isNotEmpty()) {
            itemCountDao.upsertCounts(items)
        }
    }

    // Menghapus hitungan pada list tertentu (Clear list ini)
    suspend fun clearListCounts(storeId: String, listId: String) {
        itemCountDao.clearListCounts(storeId, listId)
        undoRedoDao.deleteHistoryForList(storeId, listId)
    }

    // Menghapus seluruh hitungan pada toko tersebut (Clear semua list toko ini)
    suspend fun clearStoreCounts(storeId: String) {
        itemCountDao.clearStoreCounts(storeId)
        undoRedoDao.deleteHistoryForStore(storeId)
    }

    // ==========================================
    // METODE UNTUK RIWAYAT UNDO & REDO
    // ==========================================
    suspend fun getUndoRedoHistoryForList(storeId: String, listId: String): List<UndoRedoEntity> {
        return undoRedoDao.getHistoryForList(storeId, listId)
    }

    suspend fun saveUndoRedoStep(entity: UndoRedoEntity) {
        undoRedoDao.insert(entity)
    }

    suspend fun clearRedoStack(storeId: String, listId: String) {
        undoRedoDao.clearStackType(storeId, listId, "REDO")
    }

    suspend fun replaceUndoRedoHistoryForList(storeId: String, listId: String, history: List<UndoRedoEntity>) {
        undoRedoDao.deleteHistoryForList(storeId, listId)
        history.forEach { undoRedoDao.insert(it) }
    }
}
