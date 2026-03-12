package com.example.autodialer.data.local.dao

import androidx.room.*
import com.example.autodialer.data.local.entity.AudioFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioFileDao {
    @Query("SELECT * FROM audio_files ORDER BY addedAt DESC")
    fun getAllFiles(): Flow<List<AudioFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AudioFileEntity): Long

    @Delete
    suspend fun delete(entity: AudioFileEntity)

    @Query("DELETE FROM audio_files WHERE id = :id")
    suspend fun deleteById(id: Long)
}
