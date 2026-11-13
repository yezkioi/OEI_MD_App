package com.example.oeimdapp.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.oeimdapp.data.local.AppDatabase
import com.example.oeimdapp.data.local.ItemCountEntity
import com.example.oeimdapp.data.model.ApiResponse
import com.example.oeimdapp.data.model.ListDetailDto
import com.example.oeimdapp.data.model.StoreDto
import com.example.oeimdapp.data.prefs.SettingPreferences
import com.example.oeimdapp.data.remote.RetrofitClient
import com.example.oeimdapp.data.repository.OeiRepository
import com.google.gson.Gson
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = SettingPreferences(application)
    private val database = AppDatabase.getDatabase(application)
    private val repository = OeiRepository(RetrofitClient.apiService, database.itemCountDao())
    private val gson = Gson()

    // ==========================================
    // 1. STATE PENGATURAN & TEMA
    // ==========================================
    var themeMode: String by mutableStateOf(prefs.themeMode)
        private set

    var mdName: String by mutableStateOf(prefs.mdName)
        private set

    var webAppUrl: String by mutableStateOf(prefs.webAppUrl)
        private set

    var appToken: String by mutableStateOf(prefs.appToken)
        private set

    var lastStoreId: String by mutableStateOf(prefs.lastStoreId)
        private set

    var lastSyncTime: String by mutableStateOf(prefs.lastSyncTime)
        private set

    val isConfigured: Boolean
        get() = (webAppUrl.startsWith("http://") || webAppUrl.startsWith("https://")) &&
                appToken.isNotBlank()

    // ==========================================
    // 2. STATE DATA SINKRONISASI (OFFLINE-FIRST)
    // ==========================================
    var syncData: ApiResponse? by mutableStateOf(null)
        private set

    var isSyncing: Boolean by mutableStateOf(false)
        private set

    var syncError: String? by mutableStateOf(null)
        private set

    var syncSuccessMessage: String? by mutableStateOf(null)
        private set

    var listUpdatedBanner: String? by mutableStateOf(null)
        private set

    // ==========================================
    // 3. STATE TOKO, LIST & HITUNGAN AKTIF
    // ==========================================
    var selectedStoreId: String? by mutableStateOf(null)
        private set

    var selectedListId: String? by mutableStateOf(null)
        private set

    var selectedOrientation: String by mutableStateOf("Vertical")
        private set

    // Mencatat list mana saja yang sudah pernah dibuka MD pada tiap toko (untuk validasi Export)
    var openedListIdsByStore: Map<String, Set<String>> by mutableStateOf(emptyMap())
        private set

    // Peta hitungan stok untuk toko yang sedang dibuka: Map<listId, Map<kode, count>>
    var currentStoreCounts: Map<String, Map<String, Int>> by mutableStateOf(emptyMap())
        private set

    // Cadangan sementara untuk fitur Urungkan (5 detik) setelah Clear
    private var lastClearedBackup: List<ItemCountEntity> = emptyList()
    private var rawStoreCountEntities: List<ItemCountEntity> = emptyList()
    private var countObserverJob: Job? = null

    init {
        loadCachedData()
    }

    private fun loadCachedData() {
        val cachedJson = prefs.cachedSyncJson
        if (cachedJson.isNotBlank()) {
            try {
                syncData = gson.fromJson(cachedJson, ApiResponse::class.java)
            } catch (_: Exception) {
                // Abaikan jika cache rusak; pengguna bisa sinkron ulang
            }
        }
    }

    // ==========================================
    // 4. AKSI PENGATURAN & SINKRONISASI
    // ==========================================
    fun updateThemeMode(newMode: String) {
        themeMode = newMode
        prefs.themeMode = newMode
    }

    fun saveSettings(newMdName: String, newUrl: String, newToken: String) {
        val cleanName = newMdName.trim().ifEmpty { "MD" }
        val cleanUrl = RetrofitClient.cleanWebAppUrl(newUrl)
        val cleanToken = newToken.trim()

        mdName = cleanName
        webAppUrl = cleanUrl
        appToken = cleanToken

        prefs.mdName = cleanName
        prefs.webAppUrl = cleanUrl
        prefs.appToken = cleanToken
    }

    fun clearSyncFeedback() {
        syncError = null
        syncSuccessMessage = null
    }

    fun dismissListUpdatedBanner() {
        listUpdatedBanner = null
    }

    fun syncNow(forceRefresh: Boolean = true, onSuccess: (() -> Unit)? = null) {
        if (webAppUrl.isBlank() || appToken.isBlank()) {
            syncError = "URL Web App dan Token Akses wajib diisi terlebih dahulu."
            return
        }

        viewModelScope.launch {
            isSyncing = true
            syncError = null
            syncSuccessMessage = null
            try {
                val oldLists = syncData?.lists.orEmpty()
                val response = repository.syncFromServer(
                    webAppUrl = webAppUrl,
                    token = appToken,
                    noCache = if (forceRefresh) 1 else null
                )

                if (!response.ok) {
                    syncError = "Server menolak permintaan. Periksa kembali Token Akses."
                } else {
                    // Deteksi perubahan hash list dari WH
                    val hasListChanged = oldLists.isNotEmpty() && response.lists.any { (id, newList) ->
                        val oldList = oldLists[id]
                        oldList != null && oldList.hash != newList.hash
                    }
                    if (hasListChanged) {
                        listUpdatedBanner = "List diperbarui oleh WH"
                    }

                    syncData = response
                    prefs.cachedSyncJson = gson.toJson(response)

                    val nowText = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                    lastSyncTime = nowText
                    prefs.lastSyncTime = nowText

                    syncSuccessMessage = "Sinkronisasi berhasil (${getActiveStores().size} toko aktif)"
                    onSuccess?.invoke()
                }
            } catch (e: Exception) {
                syncError = when {
                    e.message?.contains("Unable to resolve host", ignoreCase = true) == true ->
                        "Tidak ada koneksi internet. Menggunakan data offline tersimpan."
                    e.message?.contains("404") == true ->
                        "URL Web App tidak ditemukan (404). Pastikan link berakhiran /exec."
                    else ->
                        "Gagal sinkronisasi: ${e.localizedMessage ?: "Periksa URL & koneksi internet"}"
                }
            } finally {
                isSyncing = false
            }
        }
    }

    // ==========================================
    // 5. LOGIKA TOKO & LIST (URUTAN NATURAL)
    // ==========================================
    fun getActiveStores(): List<StoreDto> {
        val data = syncData ?: return emptyList()
        return data.stores
            .filter { store -> store.listIds.any { data.lists.containsKey(it) } }
            .sortedWith { a, b -> naturalCompare(a.nama, b.nama) }
    }

    fun getStoreById(storeId: String): StoreDto? {
        return getActiveStores().find { it.id == storeId }
    }

    fun getListsForStore(store: StoreDto): List<ListDetailDto> {
        val data = syncData ?: return emptyList()
        return store.listIds
            .mapNotNull { id -> data.lists[id] }
            .sortedWith { a, b ->
                val orderDiff = a.jenisUrutan.compareTo(b.jenisUrutan)
                if (orderDiff != 0) orderDiff
                else store.listIds.indexOf(a.id).compareTo(store.listIds.indexOf(b.id))
            }
    }

    fun openStore(storeId: String) {
        val store = getStoreById(storeId) ?: return
        selectedStoreId = storeId
        lastStoreId = storeId
        prefs.lastStoreId = storeId

        // Mulai pantau perubahan angka di Room untuk toko ini
        countObserverJob?.cancel()
        countObserverJob = viewModelScope.launch {
            repository.getCountsForStore(storeId).collect { entities ->
                rawStoreCountEntities = entities
                val grouped = mutableMapOf<String, MutableMap<String, Int>>()
                for (entity in entities) {
                    val listMap = grouped.getOrPut(entity.listId) { mutableMapOf() }
                    listMap[entity.kode] = entity.count
                }
                currentStoreCounts = grouped
            }
        }

        // Buka list default (urutan_tampil terkecil, contoh: MP)
        val lists = getListsForStore(store)
        val defaultList = lists.firstOrNull()
        if (defaultList != null) {
            selectList(defaultList.id)
        } else {
            selectedListId = null
        }
    }

    fun selectList(listId: String) {
        selectedListId = listId
        val storeId = selectedStoreId ?: return

        // Tandai list ini sudah dibuka oleh MD
        val currentOpened = openedListIdsByStore[storeId].orEmpty()
        openedListIdsByStore = openedListIdsByStore + (storeId to (currentOpened + listId))

        // Atur tab orientasi default jika pisah_orientasi == true
        val listDetail = syncData?.lists?.get(listId)
        if (listDetail != null && listDetail.pisahOrientasi) {
            val hasVertical = listDetail.items.any { it.orientasi.equals("Vertical", ignoreCase = true) }
            selectedOrientation = if (hasVertical) "Vertical" else "Horizontal"
        } else {
            selectedOrientation = "Vertical"
        }
    }

    fun selectOrientation(orientation: String) {
        selectedOrientation = orientation
    }

    // ==========================================
    // 6. UPDATE ANGKA STOK & CLEAR + URUNGKAN
    // ==========================================
    fun getCount(listId: String, kode: String): Int {
        return currentStoreCounts[listId]?.get(kode) ?: 0
    }

    fun updateItemCount(listId: String, kode: String, newCount: Int) {
        val storeId = selectedStoreId ?: return
        val safeCount = newCount.coerceIn(0, 9999)
        viewModelScope.launch {
            repository.updateCount(
                storeId = storeId,
                listId = listId,
                kode = kode,
                count = safeCount
            )
        }
    }

    fun clearCurrentList() {
        val storeId = selectedStoreId ?: return
        val listId = selectedListId ?: return
        lastClearedBackup = rawStoreCountEntities.filter { it.storeId == storeId && it.listId == listId }
        viewModelScope.launch {
            repository.clearListCounts(storeId, listId)
        }
    }

    fun clearAllListsInStore() {
        val storeId = selectedStoreId ?: return
        lastClearedBackup = rawStoreCountEntities.filter { it.storeId == storeId }
        viewModelScope.launch {
            repository.clearStoreCounts(storeId)
        }
    }

    fun undoClear() {
        val backup = lastClearedBackup
        if (backup.isEmpty()) return
        lastClearedBackup = emptyList()
        viewModelScope.launch {
            repository.restoreCounts(backup)
        }
    }

    // ==========================================
    // 7. ID LAPORAN OTOMATIS (LP0001, LP0002, ...)
    // ==========================================
    fun peekNextReportId(): String {
        return String.format(Locale.US, "LP%04d", prefs.reportCounter)
    }

    fun incrementReportCounter() {
        prefs.reportCounter = prefs.reportCounter + 1
    }

    // Pembanding abjad natural agar "Toko 2" tampil sebelum "Toko 10"
    private fun naturalCompare(a: String, b: String): Int {
        val strA = a.lowercase(Locale.getDefault())
        val strB = b.lowercase(Locale.getDefault())
        var i = 0
        var j = 0

        while (i < strA.length && j < strB.length) {
            val charA = strA[i]
            val charB = strB[j]

            if (charA.isDigit() && charB.isDigit()) {
                val startA = i
                while (i < strA.length && strA[i].isDigit()) i++
                val startB = j
                while (j < strB.length && strB[j].isDigit()) j++

                val digitsA = strA.substring(startA, i).trimStart('0')
                val digitsB = strB.substring(startB, j).trimStart('0')

                if (digitsA.length != digitsB.length) {
                    return digitsA.length.compareTo(digitsB.length)
                }
                val digitDiff = digitsA.compareTo(digitsB)
                if (digitDiff != 0) return digitDiff
            } else {
                if (charA != charB) return charA.compareTo(charB)
                i++
                j++
            }
        }
        return strA.length.compareTo(strB.length)
    }
}