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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.oeimdapp.data.model.ItemDto
import com.example.oeimdapp.ui.theme.ItemCodeTextStyle
import com.example.oeimdapp.ui.theme.OeiTheme
import com.example.oeimdapp.ui.theme.TabularNumberTextStyle

// ==========================================
// 1. MODAL NUMERIC KEYPAD (KETIK ANGKA MANUAL)
// ==========================================
@Composable
fun NumericKeypadDialog(
    itemLabel: String,
    target: Int?, // null jika kartu ALIAS-000
    initialValue: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val colors = OeiTheme.colors
    var inputText: String by remember(initialValue) { mutableStateOf(initialValue.toString()) }
    var isPristine: Boolean by remember(initialValue) { mutableStateOf(true) }

    fun onDigitClick(digit: String) {
        if (isPristine) {
            inputText = digit
            isPristine = false
        } else {
            val candidate = if (inputText == "0") digit else inputText + digit
            if (candidate.length <= 4) {
                inputText = candidate
            }
        }
    }

    fun onBackspaceClick() {
        isPristine = false
        inputText = if (inputText.length > 1) inputText.dropLast(1) else "0"
    }

    fun onClearClick() {
        inputText = "0"
        isPristine = false
    }

    Dialog(onDismissRequest = onDismiss) {
        AnimatedVisibility(
            visible = true,
            enter = scaleIn(initialScale = 0.75f) + fadeIn(animationSpec = tween(220)),
            exit = scaleOut(targetScale = 0.75f) + fadeOut(animationSpec = tween(180))
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.elevatedSurface,
                border = BorderStroke(1.dp, colors.borderStrong),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Kode & Target
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = itemLabel,
                            style = ItemCodeTextStyle.copy(fontSize = 16.sp),
                            color = colors.textPrimary
                        )
                        Text(
                            text = if (target != null) "Target stok: $target pcs" else "Produk di luar list",
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = colors.textSecondary
                        )
                    }
                }

                // Kotak Tampilan Angka
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.subtleSurface)
                        .border(1.5.dp, colors.primaryAction, RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isPristine) colors.primaryAction.copy(alpha = 0.16f)
                                else Color.Transparent
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = inputText,
                            style = TabularNumberTextStyle.copy(fontSize = 28.sp),
                            color = colors.textPrimary
                        )
                    }
                }

                // Tombol Pintas "Isi Sesuai Target" (hanya untuk item reguler)
                if (target != null && target > 0) {
                    OutlinedButton(
                        onClick = { onConfirm(target.coerceIn(0, 9999)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, colors.penuhBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = colors.penuhCardBg,
                            contentColor = colors.penuhBadgeText
                        )
                    ) {
                        Text(
                            text = "Isi Sesuai Target ($target)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Grid Tombol Angka 0–9, C, dan Backspace
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "DEL")
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    rows.forEach { rowKeys ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowKeys.forEach { key ->
                                val isActionKey = key == "C" || key == "DEL"
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isActionKey) colors.subtleSurfaceAlt
                                            else colors.subtleSurface
                                        )
                                        .border(1.dp, colors.borderStrong, RoundedCornerShape(12.dp))
                                        .clickable {
                                            when (key) {
                                                "C" -> onClearClick()
                                                "DEL" -> onBackspaceClick()
                                                else -> onDigitClick(key)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (key == "DEL") {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                                            contentDescription = "Hapus satu digit",
                                            tint = colors.textPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else {
                                        Text(
                                            text = key,
                                            style = TabularNumberTextStyle.copy(fontSize = 18.sp),
                                            color = if (key == "C") colors.habisBorder else colors.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Tombol Batal & OK
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
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
                        onClick = {
                            val parsed = inputText.toIntOrNull()?.coerceIn(0, 9999) ?: 0
                            onConfirm(parsed)
                        },
                        modifier = Modifier
                            .weight(1.4f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primaryAction,
                            contentColor = colors.onPrimaryAction
                        )
                    ) {
                        Text(
                            text = "OK",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
      }
    }
}

// ==========================================
// 2. DIALOG ZOOM LIGHTBOX (PERBESAR GAMBAR)
// ==========================================
@Composable
fun ZoomLightboxDialog(
    item: ItemDto,
    showOrientation: Boolean,
    currentCount: Int,
    onUpdateCount: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = OeiTheme.colors
    var useFallbackUrl: Boolean by remember(item.fileId) { mutableStateOf(false) }

    val imageUrl = if (!useFallbackUrl) {
        "https://drive.google.com/thumbnail?id=${item.fileId}&sz=w800"
    } else {
        "https://drive.google.com/uc?export=view&id=${item.fileId}"
    }

    val lightboxImageBg = if (colors.isDark) colors.appBackground else Color.White

    val statusText: String
    val statusColor: Color
    when {
        currentCount <= 0 -> {
            statusText = "Status: Habis"
            statusColor = colors.habisBorder
        }
        currentCount < item.target -> {
            statusText = "Status: Kurang ${item.target - currentCount} pcs"
            statusColor = colors.kurangBorder
        }
        else -> {
            statusText = "Status: Penuh ✓"
            statusColor = colors.penuhBorder
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        AnimatedVisibility(
            visible = true,
            enter = scaleIn(initialScale = 0.75f) + fadeIn(animationSpec = tween(220)),
            exit = scaleOut(targetScale = 0.75f) + fadeOut(animationSpec = tween(180))
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.elevatedSurface,
                border = BorderStroke(1.dp, colors.borderStrong),
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header: Kode, Lencana Best Seller, Orientasi, Tombol Tutup
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = item.label,
                                style = ItemCodeTextStyle.copy(fontSize = 18.sp),
                                color = colors.textPrimary
                            )
                            if (item.bestSeller) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(colors.bestBadgeBg)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "★ Best Seller",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.bestBadgeText
                                    )
                                }
                            }
                        }
                        if (showOrientation && !item.orientasi.isNullOrBlank()) {
                            Text(
                                text = "Orientasi: ${item.orientasi}",
                                fontSize = 12.sp,
                                color = colors.textSecondary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = colors.textPrimary
                        )
                    }
                }

                // Bingkai Gambar Utama (16.dp radius, #FFFFFF di Light / #090D16 di Dark)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(lightboxImageBg)
                        .border(1.dp, colors.border, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.fileId.isBlank()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.BrokenImage,
                                contentDescription = null,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.label,
                                style = ItemCodeTextStyle.copy(fontSize = 14.sp),
                                color = colors.textSecondary
                            )
                        }
                    } else {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = item.label,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            loading = {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(28.dp),
                                        strokeWidth = 2.5.dp,
                                        color = colors.textSecondary
                                    )
                                }
                            },
                            error = {
                                LaunchedEffect(useFallbackUrl) {
                                    if (!useFallbackUrl) {
                                        useFallbackUrl = true
                                    }
                                }
                                if (useFallbackUrl) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.BrokenImage,
                                            contentDescription = null,
                                            tint = colors.textSecondary,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = item.label,
                                            style = ItemCodeTextStyle.copy(fontSize = 14.sp),
                                            color = colors.textSecondary
                                        )
                                    }
                                }
                            }
                        )
                    }
                }

                // Footer: Target, Status Berwarna & Stepper − [angka] +
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Target: ${item.target} pcs",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = statusText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, colors.borderStrong, RoundedCornerShape(12.dp)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(colors.subtleSurfaceAlt)
                                .clickable { onUpdateCount((currentCount - 1).coerceAtLeast(0)) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "−",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(52.dp)
                                .height(42.dp)
                                .background(colors.elevatedSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentCount.toString(),
                                style = TabularNumberTextStyle.copy(fontSize = 17.sp),
                                color = colors.textPrimary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(colors.subtleSurfaceAlt)
                                .clickable { onUpdateCount((currentCount + 1).coerceAtMost(9999)) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        }
                    }
                }
            }
        }
      }
    }
}

// ==========================================
// 3. DIALOG KONFIRMASI CLEAR (2 TINGKAT)
// ==========================================
@Composable
fun ClearConfirmDialog(
    currentListName: String,
    storeName: String,
    onClearCurrentList: () -> Unit,
    onClearAllStoreLists: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = OeiTheme.colors

    Dialog(onDismissRequest = onDismiss) {
        AnimatedVisibility(
            visible = true,
            enter = scaleIn(initialScale = 0.75f) + fadeIn(animationSpec = tween(220)),
            exit = scaleOut(targetScale = 0.75f) + fadeOut(animationSpec = tween(180))
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.elevatedSurface,
                border = BorderStroke(1.dp, colors.borderStrong),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .widthIn(min = 350.dp, max = 520.dp)
            ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Reset Hitungan Stok",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary
                )

                Text(
                    text = "Pilih cakupan hitungan yang ingin dikembalikan ke 0. Anda dapat membatalkannya selama 5 detik setelah dihapus.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )

                // Pilihan 1: Clear list ini
                OutlinedButton(
                    onClick = onClearCurrentList,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, colors.kurangBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = colors.kurangCardBg,
                        contentColor = colors.kurangBadgeText
                    )
                ) {
                    Text(
                        text = "Clear list ini ($currentListName)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }

                // Pilihan 2: Clear semua list toko ini
                Button(
                    onClick = onClearAllStoreLists,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.habisBorder,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Clear semua list toko ini ($storeName)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = "Batal",
                        color = colors.textSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
      }
    }
}