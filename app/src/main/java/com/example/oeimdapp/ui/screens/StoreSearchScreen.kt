package com.example.oeimdapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oeimdapp.data.model.ListDetailDto
import com.example.oeimdapp.data.model.StoreDto
import com.example.oeimdapp.ui.theme.OeiTheme
import com.example.oeimdapp.viewmodel.MainViewModel

@Composable
fun StoreSearchScreen(
    viewModel: MainViewModel,
    onOpenStore: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    val colors = OeiTheme.colors
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val activeStores = viewModel.getActiveStores()
    val lastOpenedStore = remember(activeStores, viewModel.lastStoreId) {
        if (viewModel.lastStoreId.isNotBlank()) {
            activeStores.find { it.id == viewModel.lastStoreId }
        } else {
            null
        }
    }

    val filteredStores = remember(activeStores, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) {
            activeStores
        } else {
            activeStores.filter { store ->
                store.nama.contains(q, ignoreCase = true) ||
                        store.area.contains(q, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBackground)
    ) {
        // ==========================================
        // TOP APP BAR
        // ==========================================
        Surface(
            color = colors.elevatedSurface,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Old East Indies MD App",
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val syncInfo = if (viewModel.lastSyncTime.isNotBlank()) {
                            "MD: ${viewModel.mdName}  •  Sync: ${viewModel.lastSyncTime}"
                        } else {
                            "MD: ${viewModel.mdName}  •  Belum sinkron"
                        }
                        Text(
                            text = syncInfo,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Tombol Sinkron Cepat
                    IconButton(
                        onClick = { viewModel.syncNow(forceRefresh = true) },
                        enabled = !viewModel.isSyncing
                    ) {
                        if (viewModel.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = colors.textPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sinkronkan Data",
                                tint = colors.textPrimary
                            )
                        }
                    }

                    // Tombol Pengaturan
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Pengaturan",
                            tint = colors.textPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ==========================================
                // KOLOM PENCARIAN REAL-TIME + TOMBOL HAPUS (×)
                // ==========================================
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "Cari nama toko atau area (mis. Ubud)...",
                            color = colors.textSecondary,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Cari",
                            tint = colors.textSecondary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Hapus pencarian",
                                    tint = colors.textSecondary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = colors.subtleSurface,
                        unfocusedContainerColor = colors.subtleSurface,
                        focusedBorderColor = colors.primaryAction,
                        unfocusedBorderColor = colors.borderStrong,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    )
                )
            }
        }

        // ==========================================
        // BANNER NOTIFIKASI PERUBAHAN LIST DARI WH
        // ==========================================
        viewModel.listUpdatedBanner?.let { bannerText ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.kurangCardBg)
                    .border(1.dp, colors.kurangBorder)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Info: $bannerText",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.kurangBadgeText,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = { viewModel.dismissListUpdatedBanner() },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "Tutup",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.kurangBadgeText
                    )
                }
            }
        }

        // Banner Galat Koneksi (jika sinkronisasi gagal)
        viewModel.syncError?.let { errorMsg ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.habisCardBg)
                    .border(1.dp, colors.habisBorder)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = errorMsg,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.habisStepperBtnText,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = { viewModel.clearSyncFeedback() },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "OK",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.habisStepperBtnText
                    )
                }
            }
        }

        // ==========================================
        // DAFTAR TOKO MITRA (DENGAN PULL-TO-REFRESH)
        // ==========================================
        @OptIn(ExperimentalMaterial3Api::class)
        PullToRefreshBox(
            isRefreshing = viewModel.isSyncing,
            onRefresh = { viewModel.syncNow(forceRefresh = true) },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            // 1. Pintasan "Toko Terakhir Dibuka" (hanya tampil saat kolom pencarian kosong)
            if (searchQuery.isBlank() && lastOpenedStore != null) {
                item(key = "last_opened_section") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Toko Terakhir Dibuka",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.textSecondary
                            )
                        }

                        StoreCardItem(
                            store = lastOpenedStore,
                            storeLists = viewModel.getListsForStore(lastOpenedStore),
                            isRecentHighlight = true,
                            onClick = {
                                viewModel.openStore(lastOpenedStore.id)
                                onOpenStore(lastOpenedStore.id)
                            }
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }

            // 2. Judul Bagian Daftar Toko
            item(key = "section_header") {
                val sectionTitle = if (searchQuery.isBlank()) {
                    "Semua Toko Mitra (${filteredStores.size})"
                } else {
                    "Hasil Pencarian (${filteredStores.size})"
                }
                Text(
                    text = sectionTitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary
                )
            }

            // 3. Kondisi Kosong (Empty State) atau Daftar Kartu Toko
            if (filteredStores.isEmpty()) {
                item(key = "empty_state") {
                    val borderColor = colors.zeroCardBorder
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.elevatedSurface)
                            .drawBehind {
                                val strokeWidth = 2.dp.toPx()
                                drawRoundRect(
                                    color = borderColor,
                                    style = Stroke(
                                        width = strokeWidth,
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                                    ),
                                    cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                                )
                            }
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (searchQuery.isNotBlank()) {
                                Text(
                                    text = "Toko tidak ditemukan",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.textPrimary,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Tidak ada nama toko atau area yang cocok dengan \"${searchQuery.trim()}\".",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textSecondary,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                Text(
                                    text = "Belum ada data toko aktif",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.textPrimary,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Tekan tombol di bawah untuk menarik daftar toko terbaru dari Google Sheets.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textSecondary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = { viewModel.syncNow(forceRefresh = true) },
                                    enabled = !viewModel.isSyncing,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.primaryAction,
                                        contentColor = colors.onPrimaryAction
                                    )
                                ) {
                                    Text(
                                        text = if (viewModel.isSyncing) "Menyinkronkan..." else "Sinkronkan Data Sekarang",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                items(
                    items = filteredStores,
                    key = { store -> store.id }
                ) { store ->
                    StoreCardItem(
                        store = store,
                        storeLists = viewModel.getListsForStore(store),
                        isRecentHighlight = false,
                        onClick = {
                            viewModel.openStore(store.id)
                            onOpenStore(store.id)
                        }
                    )
                }
            }
        }
    }
}
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StoreCardItem(
    store: StoreDto,
    storeLists: List<ListDetailDto>,
    isRecentHighlight: Boolean,
    onClick: () -> Unit
) {
    val colors = OeiTheme.colors
    val cardBorder = if (isRecentHighlight) {
        BorderStroke(1.5.dp, colors.primaryAction)
    } else {
        BorderStroke(1.dp, colors.border)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = colors.elevatedSurface,
        border = cardBorder,
        shadowElevation = if (isRecentHighlight) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Nama Toko
                Text(
                    text = store.nama,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary
                )

                // Badge Lokasi Area
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = store.area.ifBlank { "Tanpa Area" },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textSecondary
                    )
                }

                // Daftar List Aktif Toko
                if (storeLists.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        storeLists.forEach { listDetail ->
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(colors.subtleSurfaceAlt)
                                    .border(1.dp, colors.border, CircleShape)
                                    .padding(horizontal = 9.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "${listDetail.jenisAlias} • ${listDetail.nama}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Buka Toko",
                tint = colors.textSecondary
            )
        }
    }
}