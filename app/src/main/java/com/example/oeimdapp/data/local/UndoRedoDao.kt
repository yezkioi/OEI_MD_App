package com.example.oeimdapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UndoRedoDao {
    @Query("SELECT * FROM undo_redo_history WHERE storeId = :storeId AND listId = :listId ORDER BY stepOrder ASC")
    suspend fun getHistoryForList(storeId: String, listId: String): List<UndoRedoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: UndoRedoEntity)

    @Query("DELETE FROM undo_redo_history WHERE storeId = :storeId AND listId = :listId")
    suspend fun deleteHistoryForList(storeId: String, listId: String)

    @Query("DELETE FROM undo_redo_history WHERE storeId = :storeId")
    suspend fun deleteHistoryForStore(storeId: String)

    @Query("DELETE FROM undo_redo_history WHERE storeId = :storeId AND listId = :listId AND stackType = :stackType")
    suspend fun clearStackType(storeId: String, listId: String, stackType: String)

    @Query("SELECT * FROM undo_redo_history WHERE storeId = :storeId AND listId = :listId AND stackType = :stackType ORDER BY stepOrder ASC")
    suspend fun getStack(storeId: String, listId: String, stackType: String): List<UndoRedoEntity>
}
