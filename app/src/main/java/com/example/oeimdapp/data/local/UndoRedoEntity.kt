package com.example.oeimdapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "undo_redo_history")
data class UndoRedoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val storeId: String,
    val listId: String,
    val stackType: String, // "UNDO" atau "REDO"
    val stepOrder: Int,    // Urutan langkah (0 s.d. 9)
    val snapshotJson: String, // Simpan Map<String, Int> (kode -> count) dalam bentuk JSON
    val targetKode: String,  // Kode produk yang nilainya diubah pada langkah ini (untuk auto-scroll)
    val stepperAction: String = "", // "PLUS", "MINUS", atau "" (jika keypad)
    val timestamp: Long = System.currentTimeMillis()
)
