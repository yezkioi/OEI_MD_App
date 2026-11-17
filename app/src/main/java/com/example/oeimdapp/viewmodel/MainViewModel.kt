package com.example.oeimdapp.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.Coil
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.oeimdapp.data.local.AppDatabase
import com.example.oeimdapp.data.local.ItemCountEntity
import com.example.oeimdapp.data.local.UndoRedoEntity
import com.example.oeimdapp.data.model.ApiResponse
import com.example.oeimdapp.data.model.ListDetailDto
import com.example.oeimdapp.data.model.StoreDto
import com.example.oeimdapp.data.prefs.SettingPreferences
import com.example.oeimdapp.data.remote.RetrofitClient
import com.example.oeimdapp.data.repository.OeiRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = SettingPreferences(application)
    private val database = AppDatabase.getDatabase(application)
    private val repository = OeiRepository(RetrofitClient.apiService, database.itemCountDao(), database.undoRedoDao())
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
    // 2. STATE DATA SINKRONISASI & ASSET GAMBAR (OFFLINE-FIRST)
    // ==========================================
    var syncData: ApiResponse? by mutableStateOf(null)
        private set

    var isSyncing: Boolean by mutableStateOf(false)
        private set

    var globalToastMessage: String? by mutableStateOf(null)
        private set

    var globalToastType: ToastType by mutableStateOf(ToastType.SUCCESS)
        private set

    enum class ToastType {
        SUCCESS, ERROR, INFO
    }

    fun showGlobalToast(message: String, type: ToastType = ToastType.SUCCESS) {
        globalToastMessage = message
        globalToastType = type
    }

    fun dismissGlobalToast() {
        globalToastMessage = null
        syncError = null
        syncSuccessMessage = null
        listUpdatedBanner = null
        imagePreloadSuccessMessage = null
        exportToastMessage = null
    }

    var syncError: String? by mutableStateOf(null)
        private set

    var syncSuccessMessage: String? by mutableStateOf(null)
        private set

    var listUpdatedBanner: String? by mutableStateOf(null)
        private set

    var isPreloadingImages: Boolean by mutableStateOf(false)
        private set

    var preloadProgressText: String? by mutableStateOf(null)
        private set

    var imagePreloadSuccessMessage: String? by mutableStateOf(null)
        private set

    var exportToastMessage: String? by mutableStateOf(null)
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

    // ==========================================
    // 3.5. STATE RIWAYAT UNDO & REDO TERISOLASI
    // ==========================================
    data class UndoRedoTargetEvent(
        val targetKode: String,
        val listId: String,
        val stepperAction: String,
        val eventId: Long = System.nanoTime()
    )

    var canUndo: Boolean by mutableStateOf(false)
        private set

    var canRedo: Boolean by mutableStateOf(false)
        private set

    var lastUndoRedoEvent: UndoRedoTargetEvent? by mutableStateOf(null)
        private set

    fun consumeUndoRedoEvent() {
        lastUndoRedoEvent = null
    }

    private val undoStacks: MutableMap<String, MutableList<UndoRedoEntity>> = mutableMapOf()
    private val redoStacks: MutableMap<String, MutableList<UndoRedoEntity>> = mutableMapOf()

    private fun updateCanUndoRedoState() {
        val storeId = selectedStoreId
        val listId = selectedListId
        if (storeId == null || listId == null) {
            canUndo = false
            canRedo = false
            return
        }
        val key = "${storeId}_$listId"
        canUndo = (undoStacks[key]?.isNotEmpty() == true)
        canRedo = (redoStacks[key]?.isNotEmpty() == true)
    }

    private fun loadUndoRedoHistoryForList(storeId: String, listId: String) {
        val key = "${storeId}_$listId"
        viewModelScope.launch {
            val history = repository.getUndoRedoHistoryForList(storeId, listId)
            val undos = history.filter { it.stackType == "UNDO" }.sortedBy { it.stepOrder }.toMutableList()
            val redos = history.filter { it.stackType == "REDO" }.sortedBy { it.stepOrder }.toMutableList()
            undoStacks[key] = undos
            redoStacks[key] = redos
            updateCanUndoRedoState()
        }
    }

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

    private fun updateSyncError(err: String?) {
        syncError = err
        if (err != null) showGlobalToast(err, ToastType.ERROR)
    }

    private fun updateSyncSuccess(msg: String?) {
        syncSuccessMessage = msg
        if (msg != null) showGlobalToast(msg, ToastType.SUCCESS)
    }

    private fun updateListBanner(banner: String?) {
        listUpdatedBanner = banner
        if (banner != null) showGlobalToast("Info: $banner", ToastType.INFO)
    }

    private fun updateImagePreloadSuccess(msg: String?) {
        imagePreloadSuccessMessage = msg
        if (msg != null) showGlobalToast(msg, ToastType.SUCCESS)
    }

    fun clearSyncFeedback() {
        syncError = null
        syncSuccessMessage = null
    }

    fun dismissListUpdatedBanner() {
        listUpdatedBanner = null
        dismissGlobalToast()
    }

    fun dismissImagePreloadSuccess() {
        imagePreloadSuccessMessage = null
        dismissGlobalToast()
    }

    fun showExportToast(message: String) {
        exportToastMessage = message
        showGlobalToast(message, ToastType.SUCCESS)
    }

    fun dismissExportToast() {
        exportToastMessage = null
        dismissGlobalToast()
    }

    fun preloadAllImages(context: Context) {
        if (webAppUrl.isBlank() || appToken.isBlank()) {
            updateSyncError("URL Web App dan Token Akses wajib diisi terlebih dahulu.")
            return
        }

        viewModelScope.launch {
            isPreloadingImages = true
            preloadProgressText = "Menyinkronkan data..."
            updateImagePreloadSuccess(null)
            updateSyncError(null)

            try {
                // 1. Sinkronisasi data terbaru terlebih dahulu di latar belakang
                val response = repository.syncFromServer(
                    webAppUrl = webAppUrl,
                    token = appToken,
                    noCache = 1
                )

                if (!response.ok) {
                    updateSyncError("Server menolak permintaan. Periksa kembali Token Akses.")
                } else {
                    val oldLists = syncData?.lists.orEmpty()
                    val hasListChanged = oldLists.isEmpty() || oldLists.any { (id, oldList) ->
                        val newList = response.lists[id]
                        newList == null || oldList.hash != newList.hash
                    } || response.lists.any { (id, _) -> oldLists[id] == null }

                    if (hasListChanged) {
                        updateListBanner("List diperbarui oleh WH")
                    }

                    syncData = response
                    prefs.cachedSyncJson = gson.toJson(response)

                    val nowText = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                    lastSyncTime = nowText
                    prefs.lastSyncTime = nowText

                    // 2. Kumpulkan seluruh file_id unik dari seluruh lists
                    val allFileIds = response.lists.values
                        .flatMap { listDetail -> listDetail.items.mapNotNull { it.fileId.ifBlank { null } } }
                        .distinct()

                    if (allFileIds.isEmpty()) {
                        updateImagePreloadSuccess("Tidak ada asset gambar untuk diunduh ✓")
                    } else {
                        val total = allFileIds.size
                        val imageLoader = Coil.imageLoader(context)

                        withContext(Dispatchers.IO) {
                            allFileIds.forEachIndexed { index, fileId ->
                                preloadProgressText = "Mengunduh gambar (${index + 1}/$total)..."

                                val urlW400 = "https://drive.google.com/thumbnail?id=$fileId&sz=w400"
                                val urlW800 = "https://drive.google.com/thumbnail?id=$fileId&sz=w800"

                                runCatching {
                                    imageLoader.execute(
                                        ImageRequest.Builder(context)
                                            .data(urlW400)
                                            .memoryCachePolicy(CachePolicy.ENABLED)
                                            .diskCachePolicy(CachePolicy.ENABLED)
                                            .build()
                                    )
                                }

                                runCatching {
                                    imageLoader.execute(
                                        ImageRequest.Builder(context)
                                            .data(urlW800)
                                            .memoryCachePolicy(CachePolicy.ENABLED)
                                            .diskCachePolicy(CachePolicy.ENABLED)
                                            .build()
                                    )
                                }
                            }
                        }
                        updateImagePreloadSuccess("Asset sudah berhasil disimpan ✓")
                    }
                }
            } catch (e: Exception) {
                val err = when {
                    e.message?.contains("Unable to resolve host", ignoreCase = true) == true ->
                        "Tidak ada koneksi internet. Gagal memuat asset gambar."
                    else ->
                        "Gagal memuat gambar: ${e.localizedMessage ?: "Periksa koneksi internet"}"
                }
                updateSyncError(err)
            } finally {
                isPreloadingImages = false
                preloadProgressText = null
            }
        }
    }

    fun syncNow(forceRefresh: Boolean = true, onSuccess: (() -> Unit)? = null) {
        if (webAppUrl.isBlank() || appToken.isBlank()) {
            updateSyncError("URL Web App dan Token Akses wajib diisi terlebih dahulu.")
            return
        }

        viewModelScope.launch {
            isSyncing = true
            updateSyncError(null)
            updateSyncSuccess(null)
            try {
                val oldLists = syncData?.lists.orEmpty()
                val response = repository.syncFromServer(
                    webAppUrl = webAppUrl,
                    token = appToken,
                    noCache = if (forceRefresh) 1 else null
                )

                if (!response.ok) {
                    updateSyncError("Server menolak permintaan. Periksa kembali Token Akses.")
                } else {
                    // Deteksi perubahan hash list dari WH
                    val hasListChanged = oldLists.isEmpty() || oldLists.any { (id, oldList) ->
                        val newList = response.lists[id]
                        newList == null || oldList.hash != newList.hash
                    } || response.lists.any { (id, _) -> oldLists[id] == null }

                    if (hasListChanged) {
                        updateListBanner("List diperbarui oleh WH")
                    }

                    syncData = response
                    prefs.cachedSyncJson = gson.toJson(response)

                    val nowText = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                    lastSyncTime = nowText
                    prefs.lastSyncTime = nowText

                    val successMsg = if (hasListChanged) {
                        "Sinkronisasi berhasil — List diperbarui oleh WH (${getActiveStores().size} toko)"
                    } else {
                        "Sinkronisasi berhasil (${getActiveStores().size} toko aktif)"
                    }
                    updateSyncSuccess(successMsg)
                    onSuccess?.invoke()
                }
            } catch (e: Exception) {
                val err = when {
                    e.message?.contains("Unable to resolve host", ignoreCase = true) == true ->
                        "Tidak ada koneksi internet. Menggunakan data offline tersimpan."
                    e.message?.contains("404") == true ->
                        "URL Web App tidak ditemukan (404). Pastikan link berakhiran /exec."
                    else ->
                        "Gagal sinkronisasi: ${e.localizedMessage ?: "Periksa URL & koneksi internet"}"
                }
                updateSyncError(err)
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

        loadUndoRedoHistoryForList(storeId, listId)
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

    private fun getFullListSnapshot(listId: String): Map<String, Int> {
        val listDetail = syncData?.lists?.get(listId)
        val fullSnapshot = mutableMapOf<String, Int>()
        val currentCounts = currentStoreCounts[listId].orEmpty()
        if (listDetail != null) {
            listDetail.items.forEach { item ->
                fullSnapshot[item.kode] = currentCounts[item.kode] ?: 0
            }
            currentCounts.forEach { (k, v) ->
                if (k.startsWith("000")) {
                    fullSnapshot[k] = v
                }
            }
        } else {
            fullSnapshot.putAll(currentCounts)
        }
        return fullSnapshot
    }

    fun updateItemCount(listId: String, kode: String, newCount: Int, stepperAction: String = "") {
        val storeId = selectedStoreId ?: return
        val safeCount = newCount.coerceIn(0, 9999)
        val oldCount = getCount(listId, kode)

        if (oldCount == safeCount) return

        val key = "${storeId}_$listId"
        val fullSnapshot = getFullListSnapshot(listId)

        // Catat snapshot data SEBELUM perubahan ke Undo Stack
        val undoEntity = UndoRedoEntity(
            storeId = storeId,
            listId = listId,
            stackType = "UNDO",
            stepOrder = undoStacks[key]?.size ?: 0,
            snapshotJson = gson.toJson(fullSnapshot),
            targetKode = kode,
            stepperAction = stepperAction
        )

        val undoStack = undoStacks.getOrPut(key) { mutableListOf() }
        undoStack.add(undoEntity)
        if (undoStack.size > 10) {
            undoStack.removeAt(0)
        }

        // Kosongkan Redo Stack setiap kali ada perubahan baru
        redoStacks[key]?.clear()
        viewModelScope.launch {
            repository.clearRedoStack(storeId, listId)
        }

        updateCanUndoRedoState()

        viewModelScope.launch {
            repository.updateCount(
                storeId = storeId,
                listId = listId,
                kode = kode,
                count = safeCount
            )
            persistUndoRedoStacks(storeId, listId)
        }
    }

    fun performUndo() {
        val storeId = selectedStoreId ?: return
        val listId = selectedListId ?: return
        val key = "${storeId}_$listId"

        val undoStack = undoStacks[key] ?: return
        if (undoStack.isEmpty()) return

        val undoEntity = undoStack.removeAt(undoStack.lastIndex)

        // Simpan kondisi saat ini ke Redo Stack sebelum memulihkan snapshot
        val fullSnapshot = getFullListSnapshot(listId)
        val redoEntity = UndoRedoEntity(
            storeId = storeId,
            listId = listId,
            stackType = "REDO",
            stepOrder = redoStacks[key]?.size ?: 0,
            snapshotJson = gson.toJson(fullSnapshot),
            targetKode = undoEntity.targetKode,
            stepperAction = undoEntity.stepperAction
        )
        val redoStack = redoStacks.getOrPut(key) { mutableListOf() }
        redoStack.add(redoEntity)
        if (redoStack.size > 10) redoStack.removeAt(0)

        // Pulihkan snapshot dari Undo
        val snapshotType = object : com.google.gson.reflect.TypeToken<Map<String, Int>>() {}.type
        val snapshotMap: Map<String, Int> = try {
            gson.fromJson(undoEntity.snapshotJson, snapshotType)
        } catch (_: Exception) {
            emptyMap()
        }

        viewModelScope.launch {
            snapshotMap.forEach { (kode, count) ->
                repository.updateCount(storeId, listId, kode, count)
            }
            persistUndoRedoStacks(storeId, listId)
        }

        lastUndoRedoEvent = UndoRedoTargetEvent(
            targetKode = undoEntity.targetKode,
            listId = listId,
            stepperAction = undoEntity.stepperAction
        )
        updateCanUndoRedoState()
    }

    fun performRedo() {
        val storeId = selectedStoreId ?: return
        val listId = selectedListId ?: return
        val key = "${storeId}_$listId"

        val redoStack = redoStacks[key] ?: return
        if (redoStack.isEmpty()) return

        val redoEntity = redoStack.removeAt(redoStack.lastIndex)

        // Simpan kondisi saat ini ke Undo Stack sebelum memulihkan snapshot
        val fullSnapshot = getFullListSnapshot(listId)
        val undoEntity = UndoRedoEntity(
            storeId = storeId,
            listId = listId,
            stackType = "UNDO",
            stepOrder = undoStacks[key]?.size ?: 0,
            snapshotJson = gson.toJson(fullSnapshot),
            targetKode = redoEntity.targetKode,
            stepperAction = redoEntity.stepperAction
        )
        val undoStack = undoStacks.getOrPut(key) { mutableListOf() }
        undoStack.add(undoEntity)
        if (undoStack.size > 10) undoStack.removeAt(0)

        // Pulihkan snapshot dari Redo
        val snapshotType = object : com.google.gson.reflect.TypeToken<Map<String, Int>>() {}.type
        val snapshotMap: Map<String, Int> = try {
            gson.fromJson(redoEntity.snapshotJson, snapshotType)
        } catch (_: Exception) {
            emptyMap()
        }

        viewModelScope.launch {
            snapshotMap.forEach { (kode, count) ->
                repository.updateCount(storeId, listId, kode, count)
            }
            persistUndoRedoStacks(storeId, listId)
        }

        lastUndoRedoEvent = UndoRedoTargetEvent(
            targetKode = redoEntity.targetKode,
            listId = listId,
            stepperAction = redoEntity.stepperAction
        )
        updateCanUndoRedoState()
    }

    private fun persistUndoRedoStacks(storeId: String, listId: String) {
        val key = "${storeId}_$listId"
        val undoList = undoStacks[key].orEmpty()
        val redoList = redoStacks[key].orEmpty()
        val combined = mutableListOf<UndoRedoEntity>()

        undoList.forEachIndexed { index, entity ->
            combined.add(entity.copy(id = 0, stepOrder = index))
        }
        redoList.forEachIndexed { index, entity ->
            combined.add(entity.copy(id = 0, stepOrder = index))
        }

        viewModelScope.launch {
            repository.replaceUndoRedoHistoryForList(storeId, listId, combined)
        }
    }

    fun clearCurrentList() {
        val storeId = selectedStoreId ?: return
        val listId = selectedListId ?: return
        val key = "${storeId}_$listId"
        lastClearedBackup = rawStoreCountEntities.filter { it.storeId == storeId && it.listId == listId }
        undoStacks.remove(key)
        redoStacks.remove(key)
        updateCanUndoRedoState()
        viewModelScope.launch {
            repository.clearListCounts(storeId, listId)
        }
    }

    fun clearAllListsInStore() {
        val storeId = selectedStoreId ?: return
        lastClearedBackup = rawStoreCountEntities.filter { it.storeId == storeId }
        val keysToRemove = undoStacks.keys.filter { it.startsWith("${storeId}_") }
        keysToRemove.forEach {
            undoStacks.remove(it)
            redoStacks.remove(it)
        }
        updateCanUndoRedoState()
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