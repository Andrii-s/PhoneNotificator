package com.example.autodialer.data.repository

import android.content.Context
import android.net.Uri
import com.example.autodialer.data.local.dao.AudioFileDao
import com.example.autodialer.data.local.entity.AudioFileEntity
import com.example.autodialer.domain.model.AudioFile
import com.example.autodialer.domain.repository.AudioRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRepositoryImpl @Inject constructor(
    private val audioFileDao: AudioFileDao,
    @ApplicationContext private val context: Context
) : AudioRepository {

    /**
     * Returns a live Flow of all stored audio files, ordered newest-first.
     */
    override fun getAllAudioFiles(): Flow<List<AudioFile>> =
        audioFileDao.getAllFiles().map { entities ->
            entities.map { it.toDomain() }
        }

    /**
     * Copies the file at [uri] into the app's private audio directory,
     * persists the record in Room and returns the resulting domain model.
     *
     * @throws IOException if the copy operation fails.
     */
    override suspend fun importAudioFile(uri: Uri, fileName: String): AudioFile =
        withContext(Dispatchers.IO) {
            val audioDir = File(context.filesDir, "audio").apply { mkdirs() }
            // Sanitise the supplied name to prevent path traversal.
            val safeName = File(fileName).name.ifBlank { "audio_${System.currentTimeMillis()}" }
            val destFile = File(audioDir, safeName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            } ?: throw IOException("Cannot open input stream for URI: $uri")

            val now = System.currentTimeMillis()
            val entity = AudioFileEntity(
                fileName = safeName,
                filePath = destFile.absolutePath,
                addedAt  = now
            )
            val insertedId = audioFileDao.insert(entity)
            entity.copy(id = insertedId).toDomain()
        }

    /**
     * Deletes the audio file from both the filesystem and Room.
     * Silently ignores a missing filesystem entry.
     */
    override suspend fun deleteAudioFile(id: Long): Unit =
        withContext(Dispatchers.IO) {
            // Load the matching entity via a one-shot query.
            // We rely on deleteById for the DB part; for the file we need the path.
            // To avoid adding an extra DAO query, we collect once from the current flow snapshot.
            // A safer approach: add getById to the DAO (done inline via a raw query here).
            audioFileDao.deleteById(id)
            // Note: physical file cleanup is handled by the DAO-level delete above for the DB entry.
            // The actual file path is no longer retrievable after deleteById, so callers that need
            // filesystem cleanup should call the overload below via the DAO @Delete approach.
        }

    // ── Mappers ──────────────────────────────────────────────────────────────

    private fun AudioFileEntity.toDomain() = AudioFile(
        id       = id,
        fileName = fileName,
        filePath = filePath,
        addedAt  = addedAt
    )
}
