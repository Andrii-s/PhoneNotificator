package com.example.autodialer.domain.repository

import android.net.Uri
import com.example.autodialer.domain.model.AudioFile
import kotlinx.coroutines.flow.Flow

interface AudioRepository {
    fun getAllAudioFiles(): Flow<List<AudioFile>>
    suspend fun importAudioFile(uri: Uri, fileName: String): AudioFile
    suspend fun deleteAudioFile(id: Long)
}
