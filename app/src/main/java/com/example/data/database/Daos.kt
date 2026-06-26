package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerDao {
    @Query("SELECT * FROM servers ORDER BY name ASC")
    fun getAllServers(): Flow<List<ServerEntity>>

    @Query("SELECT * FROM servers WHERE id = :id LIMIT 1")
    suspend fun getServerById(id: Int): ServerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: ServerEntity): Long

    @Update
    suspend fun updateServer(server: ServerEntity)

    @Delete
    suspend fun deleteServer(server: ServerEntity)
}

@Dao
interface NotebookDao {
    @Query("SELECT * FROM notebooks ORDER BY isPinned DESC, lastOpened DESC")
    fun getAllNotebooks(): Flow<List<NotebookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotebook(notebook: NotebookEntity): Long

    @Update
    suspend fun updateNotebook(notebook: NotebookEntity)

    @Delete
    suspend fun deleteNotebook(notebook: NotebookEntity)

    @Query("UPDATE notebooks SET lastOpened = :timestamp WHERE id = :id")
    suspend fun updateLastOpened(id: Int, timestamp: Long)
}
