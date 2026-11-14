package com.example.oeimdapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.oeimdapp.export.ExportListStatus
import com.example.oeimdapp.export.XlsxExporter
import com.example.oeimdapp.ui.theme.ItemCodeTextStyle
import com.example.oeimdapp.ui.theme.OeiTheme
import com.example.oeimdapp.ui.theme.TabularBadgeTextStyle
import com.example.oeimdapp.viewmodel.MainViewModel

@Composable
fun ExportVerificationDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val colors = OeiTheme.colors
    val context = LocalContext.current

    val storeId = viewModel.selectedStoreId ?: return
    val store = viewModel.getStoreById(storeId) ?: return
    val storeLists = viewModel.getListsForStore(store)

    val reportId = remember(storeId) { viewModel.peekNextReportId() }
    val previewData = remember(storeId, viewModel.currentStoreCounts, viewModel.openedListIdsByStore, viewModel.mdName) {
        XlsxExporter.buildPreviewData(
            store = store,
            storeLists = storeLists,
            countsByList = viewModel.currentStoreCounts,
            openedListIds = viewModel.openedListIdsByStore[storeId].orEmpty(),
            mdName = viewModel.mdName,
            reportId = reportId
        )
    }

    var isChecked by remember { mutableStateOf(false) }
    var isPreviewStep by remember { mutableStateOf(false) }
    var hasIncrementedCounter by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var isFeedbackError by remember { mutableStateOf(false) }

    val hasUnopenedOrAllZero = previewData.listSummaries.any {
        it.status == ExportListStatus.BELUM_DIBUKA || it.status == ExportListStatus.SEMUA_NOL
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AnimatedVisibility(
            visible = true,
            enter = scaleIn(initialScale = 0.75f) + fadeIn(animationSpec = tween(220)),
            exit = scaleOut(targetScale = 0.75f) + fadeOut(animationSpec = tween(180))
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
                    .padding(horizontal = 14.dp),
                shape = RoundedCornerShape(20.dp),
                color = colors.elevatedSurface,
                border = BorderStroke(1.dp, colors.borderStrong),
            shadowElevation = 10.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // ==========================================
                // HEADER DIALOG
                // ==========================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (!isPreviewStep) "Verifikasi Sebelum Export" else "Pratinjau Laporan XLSX",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                        Text(
                            text = previewData.fileName,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = colors.textSecondary
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = colors.textPrimary
                        )
                    }
                }

                // ==========================================
                // KONTEN DIALOG (VERIFIKASI / PRATINJAU)
                // ==========================================
                if (!isPreviewStep) {
                    // TAHAP 1: DAFTAR STATUS LIST & KOTAK CENTANG WAJIB
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 18.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Kartu Info Nama File Otomatis
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.subtleSurface)
                                .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Nama File Otomatis",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textSecondary
                                )
                                Text(
                                    text = previewData.fileName,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                            }
                        }

                        // Banner Peringatan jika ada list Belum dibuka / Semua 0
                        if (hasUnopenedOrAllZero) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.kurangCardBg)
                                    .border(1.dp, colors.kurangBorder, RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = colors.kurangBorder,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Ada list yang belum dibuka atau semua item bernilai 0 (akan tercatat habis di laporan).",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.kurangBadgeText
                                )
                            }
                        }

                        Text(
                            text = "Status Pemeriksaan Tiap List",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textSecondary
                        )

                        // Daftar Status Tiap List
                        previewData.listSummaries.forEach { summary ->
                            val badgeLabel: String
                            val badgeBg: Color
                            val badgeText: Color

                            when (summary.status) {
                                ExportListStatus.BELUM_DIBUKA -> {
                                    badgeLabel = "Belum dibuka"
                                    badgeBg = colors.habisBadgeBg
                                    badgeText = colors.habisBadgeText
                                }
                                ExportListStatus.SEMUA_NOL -> {
                                    badgeLabel = "Semua 0"
                                    badgeBg = colors.kurangBadgeBg
                                    badgeText = colors.kurangBadgeText
                                }
                                ExportListStatus.TERISI -> {
                                    badgeLabel = "Terisi (${summary.filledCount}/${summary.totalCount})"
                                    badgeBg = colors.penuhBadgeBg
                                    badgeText = colors.penuhBadgeText
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.subtleSurface)
                                    .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = summary.listName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(badgeBg)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = badgeLabel,
                                        style = TabularBadgeTextStyle,
                                        color = badgeText
                                    )
                                }
                            }
                        }

                        // Kotak Centang Wajib
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.subtleSurfaceAlt)
                                .border(
                                    width = 1.dp,
                                    color = if (isChecked) colors.primaryAction else colors.borderStrong,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { isChecked = !isChecked }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { isChecked = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = colors.primaryAction,
                                    checkmarkColor = colors.onPrimaryAction
                                )
                            )
                            Text(
                                text = "Saya sudah memeriksa semua list",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        }
                    }

                    // Tombol Buat File XLSX
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, colors.borderStrong)
                        ) {
                            Text(
                                text = "Batal",
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textSecondary
                            )
                        }

                        Button(
                            onClick = { isPreviewStep = true },
                            enabled = isChecked,
                            modifier = Modifier
                                .weight(1.5f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primaryAction,
                                contentColor = colors.onPrimaryAction
                            )
                        ) {
                            Text(
                                text = "Buat File XLSX",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    // TAHAP 2: PRATINJAU SHEET & TOMBOL SIMPAN DOWNLOAD / BAGIKAN FILE
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Lembar Pratinjau Putih ala Spreadsheet (Geser Horizontal untuk lihat seluruh kolom)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .border(1.dp, colors.borderStrong, RoundedCornerShape(12.dp))
                                .horizontalScroll(rememberScrollState())
                                .padding(12.dp)
                        ) {
                            Column {
                                // Header Laporan
                                Text(
                                    text = "LAPORAN PENGECEKAN STOK",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Toko         : ${previewData.storeName}",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Nama MD      : ${previewData.mdName}",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Tanggal cek  : ${previewData.dateSlash}",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "ID Laporan   : ${previewData.reportId}",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF0F172A)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                previewData.tables.forEachIndexed { tableIdx, table ->
                                    if (tableIdx > 0) {
                                        // Jarak bersih 4 baris kosong tanpa teks label pemisah
                                        Spacer(modifier = Modifier.height(28.dp))
                                    }

                                    // Judul Tabel
                                    Text(
                                        text = table.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Baris Header Kolom
                                    Row(
                                        modifier = Modifier
                                            .background(Color(0xFFF1F5F9))
                                            .border(1.dp, Color(0xFFCBD5E1))
                                    ) {
                                        PreviewCell("Kode Produk", 165, isBold = true, textAlign = TextAlign.Start)
                                        PreviewCell("Jumlah Target", 95, isBold = true)
                                        PreviewCell("Jumlah Dihitung", 105, isBold = true)
                                        PreviewCell("Stok Dibutuhkan", 110, isBold = true)
                                        PreviewCell("Best Seller", 80, isBold = true)
                                    }

                                    // Baris Data
                                    table.rows.forEach { row ->
                                        val rowBg = when {
                                            row.isHabis -> Color(0xFFFEE2E2)
                                            else -> Color.White
                                        }
                                        val rowTextColor = when {
                                            row.isHabis -> Color(0xFF991B1B)
                                            else -> Color(0xFF0F172A)
                                        }
                                        val neededBg = when {
                                            row.isHabis || row.isKurang -> Color(0xFFFEE2E2)
                                            else -> Color.White
                                        }
                                        val neededTextColor = when {
                                            row.isHabis || row.isKurang -> Color(0xFF991B1B)
                                            else -> Color(0xFF0F172A)
                                        }

                                        Row(
                                            modifier = Modifier
                                                .background(rowBg)
                                                .border(
                                                    width = if (row.isZeroRow) 1.5.dp else 0.5.dp,
                                                    color = if (row.isZeroRow) Color(0xFF0F172A) else Color(0xFFCBD5E1)
                                                )
                                        ) {
                                            PreviewCell(
                                                text = row.label,
                                                widthDp = 165,
                                                isBold = row.isHabis || row.isZeroRow,
                                                textColor = rowTextColor,
                                                textAlign = TextAlign.Start,
                                                isCode = true
                                            )
                                            PreviewCell(
                                                text = row.targetText,
                                                widthDp = 95,
                                                isBold = row.isHabis || row.isZeroRow,
                                                textColor = rowTextColor
                                            )
                                            PreviewCell(
                                                text = row.counted.toString(),
                                                widthDp = 105,
                                                isBold = row.isHabis || row.isZeroRow,
                                                textColor = rowTextColor
                                            )
                                            PreviewCell(
                                                text = row.neededText,
                                                widthDp = 110,
                                                isBold = row.isHabis || row.isKurang || row.isZeroRow,
                                                textColor = neededTextColor,
                                                bgColor = neededBg
                                            )
                                            PreviewCell(
                                                text = row.bestSellerText,
                                                widthDp = 80,
                                                isBold = row.isHabis || row.isZeroRow,
                                                textColor = rowTextColor
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Banner Umpan Balik Simpan / Bagikan
                        feedbackMessage?.let { msg ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isFeedbackError) colors.habisCardBg else colors.penuhCardBg)
                                    .border(
                                        1.dp,
                                        if (isFeedbackError) colors.habisBorder else colors.penuhBorder,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = msg,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isFeedbackError) colors.habisBadgeText else colors.penuhBadgeText
                                )
                            }
                        }
                    }

                    // Tombol Simpan Download & Bagikan File
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val result = XlsxExporter.saveToDownloads(context, previewData)
                                    result.onSuccess { successMsg ->
                                        if (!hasIncrementedCounter) {
                                            viewModel.incrementReportCounter()
                                            hasIncrementedCounter = true
                                        }
                                        isFeedbackError = false
                                        feedbackMessage = successMsg
                                    }.onFailure { err ->
                                        isFeedbackError = true
                                        feedbackMessage = "Gagal menyimpan: ${err.localizedMessage}"
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.5.dp, colors.primaryAction)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = null,
                                    tint = colors.textPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Simpan Download",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                            }

                            Button(
                                onClick = {
                                    val result = XlsxExporter.shareXlsxFile(context, previewData)
                                    result.onSuccess {
                                        if (!hasIncrementedCounter) {
                                            viewModel.incrementReportCounter()
                                            hasIncrementedCounter = true
                                        }
                                        isFeedbackError = false
                                        feedbackMessage = "File ${previewData.fileName} siap dibagikan."
                                    }.onFailure { err ->
                                        isFeedbackError = true
                                        feedbackMessage = "Gagal membagikan file: ${err.localizedMessage}"
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.primaryAction,
                                    contentColor = colors.onPrimaryAction
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Bagikan File",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun PreviewCell(
    text: String,
    widthDp: Int,
    isBold: Boolean = false,
    textColor: Color = Color(0xFF0F172A),
    bgColor: Color = Color.Transparent,
    textAlign: TextAlign = TextAlign.Center,
    isCode: Boolean = false
) {
    Box(
        modifier = Modifier
            .width(widthDp.dp)
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 5.dp),
        contentAlignment = if (textAlign == TextAlign.Start) Alignment.CenterStart else Alignment.Center
    ) {
        Text(
            text = text,
            style = if (isCode) ItemCodeTextStyle.copy(fontSize = 11.sp) else TabularBadgeTextStyle,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = textColor,
            textAlign = textAlign,
            maxLines = 1
        )
    }
}