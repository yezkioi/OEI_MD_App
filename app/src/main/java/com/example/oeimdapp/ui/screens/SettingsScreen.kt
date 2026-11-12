package com.example.oeimdapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oeimdapp.ui.theme.OeiTheme
import com.example.oeimdapp.viewmodel.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    canNavigateBack: Boolean,
    onNavigateBack: () -> Unit
) {
    val colors = OeiTheme.colors

    var mdNameInput by rememberSaveable { mutableStateOf(viewModel.mdName) }
    var urlInput by rememberSaveable { mutableStateOf(viewModel.webAppUrl) }
    var tokenInput by rememberSaveable { mutableStateOf(viewModel.appToken) }
    var isTokenVisible by rememberSaveable { mutableStateOf(false) }

    val themeOptions = listOf(
        "system" to "Ikuti Sistem",
        "light" to "Terang",
        "dark" to "Gelap"
    )

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (canNavigateBack) {
                    IconButton(
                        onClick = {
                            viewModel.clearSyncFeedback()
                            onNavigateBack()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = colors.textPrimary
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Pengaturan Aplikasi",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Profil MD, tema layar & koneksi Google Sheets",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary
                    )
                }
            }
        }

        // ==========================================
        // KONTEN UTAMA (SCROLLABLE)
        // ==========================================
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ------------------------------------------
            // 1. TEMA TAMPILAN (3 TOMBOL INTUITIF)
            // ------------------------------------------
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.elevatedSurface,
                border = BorderStroke(1.dp, colors.border)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Tema Tampilan",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.subtleSurface)
                            .border(1.dp, colors.borderStrong, RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        themeOptions.forEach { (modeKey, label) ->
                            val isSelected = viewModel.themeMode == modeKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(
                                        if (isSelected) colors.primaryAction
                                        else colors.subtleSurface
                                    )
                                    .clickable { viewModel.updateThemeMode(modeKey) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) colors.onPrimaryAction else colors.textSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // ------------------------------------------
            // 2. PROFIL MERCHANDISER (MD)
            // ------------------------------------------
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.elevatedSurface,
                border = BorderStroke(1.dp, colors.border)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Profil Merchandiser (MD)",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )

                    OutlinedTextField(
                        value = mdNameInput,
                        onValueChange = { mdNameInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nama Merchandiser (MD)") },
                        placeholder = { Text("Contoh: Budi") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primaryAction,
                            unfocusedBorderColor = colors.borderStrong,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        )
                    )
                }
            }

            // ------------------------------------------
            // 3. SINKRONISASI GOOGLE SHEETS (URL + TOKEN)
            // ------------------------------------------
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.elevatedSurface,
                border = BorderStroke(1.dp, colors.border)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sinkronisasi Google Sheets",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )

                        if (viewModel.lastSyncTime.isNotBlank()) {
                            Text(
                                text = "Update: ${viewModel.lastSyncTime}",
                                fontSize = 11.sp,
                                color = colors.textSecondary
                            )
                        }
                    }

                    // Kolom 1: URL Web App (Link Script)
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = {
                            urlInput = it
                            viewModel.clearSyncFeedback()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("URL Web App (Link Script)") },
                        placeholder = { Text("https://script.google.com/macros/s/.../exec") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primaryAction,
                            unfocusedBorderColor = colors.borderStrong,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        )
                    )

                    // Kolom 2: Token Akses
                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = {
                            tokenInput = it
                            viewModel.clearSyncFeedback()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Token Akses") },
                        placeholder = { Text("Masukkan token dari Script Properties") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = if (isTokenVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            TextButton(onClick = { isTokenVisible = !isTokenVisible }) {
                                Text(
                                    text = if (isTokenVisible) "Tutup" else "Lihat",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textSecondary
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primaryAction,
                            unfocusedBorderColor = colors.borderStrong,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        )
                    )

                    // Tombol Sinkronkan Data Sekarang
                    OutlinedButton(
                        onClick = {
                            viewModel.saveSettings(mdNameInput, urlInput, tokenInput)
                            viewModel.syncNow(forceRefresh = true)
                        },
                        enabled = !viewModel.isSyncing && urlInput.isNotBlank() && tokenInput.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, colors.primaryAction)
                    ) {
                        if (viewModel.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Menyinkronkan data...",
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = colors.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sinkronkan Data Sekarang",
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        }
                    }

                    // Banner Hasil Sinkronisasi (Sukses / Gagal)
                    viewModel.syncSuccessMessage?.let { msg ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.penuhCardBg)
                                .border(1.dp, colors.penuhBorder, RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = msg,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.penuhBadgeText
                            )
                        }
                    }

                    viewModel.syncError?.let { err ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.habisCardBg)
                                .border(1.dp, colors.habisBorder, RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = err,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.habisBadgeText
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // ==========================================
        // TOMBOL AKSI UTAMA DI BAWAH
        // ==========================================
        Surface(
            color = colors.elevatedSurface,
            shadowElevation = 6.dp
        ) {
            Button(
                onClick = {
                    viewModel.saveSettings(mdNameInput, urlInput, tokenInput)
                    viewModel.clearSyncFeedback()
                    if (viewModel.syncData == null && urlInput.isNotBlank() && tokenInput.isNotBlank()) {
                        viewModel.syncNow(forceRefresh = true) {
                            onNavigateBack()
                        }
                    } else {
                        onNavigateBack()
                    }
                },
                enabled = urlInput.isNotBlank() && tokenInput.isNotBlank() && !viewModel.isSyncing,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primaryAction,
                    contentColor = colors.onPrimaryAction
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Simpan & Kembali",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}