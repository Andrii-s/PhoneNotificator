package com.example.autodialer.ui.settings

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.autodialer.domain.model.AudioFile
import com.example.autodialer.domain.repository.AudioRepository
import com.example.autodialer.data.local.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the Settings screen.
 *
 * Manages the list of imported audio files and the built-in audio player
 * that lets the user preview a selected file before starting the dialing session.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val audioRepository: AudioRepository,
    @ApplicationContext private val context: Context,
    private val appPreferences: AppPreferences,
) : ViewModel() {

    // -------------------------------------------------------------------------
    // UI State
    // -------------------------------------------------------------------------

    data class SettingsUiState(
        val audioFiles: List<AudioFile> = emptyList(),
        /** ID of the file currently selected in the list (RadioButton). */
        val selectedFileId: Long? = null,
        val isPlaying: Boolean = false,
        /** Current playback position in milliseconds. */
        val currentPosition: Int = 0,
        /** Total duration of the selected file in milliseconds. */
        val duration: Int = 0,
        val isLoading: Boolean = false,
        val error: String? = null,
        /** Raw text currently shown in the audio delay input field. */
        val audioDelayInput: String = AppPreferences.DEFAULT_AUDIO_DELAY_SECONDS.toString(),
        /** True when the field contains an invalid (non-integer / negative) value. */
        val audioDelayError: Boolean = false,
    )

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // -------------------------------------------------------------------------
    // MediaPlayer state
    // -------------------------------------------------------------------------

    private var mediaPlayer: MediaPlayer? = null
    private var positionUpdateJob: Job? = null

    // -------------------------------------------------------------------------
    // Init
    // -------------------------------------------------------------------------

    init {
        loadAudioFiles()
        // Load persisted delay from SharedPreferences
        _uiState.update { it.copy(audioDelayInput = appPreferences.audioDelaySeconds.toString()) }
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /** Starts collecting audio files from the repository. */
    fun loadAudioFiles() {
        viewModelScope.launch {
            audioRepository.getAllAudioFiles().collect { files ->
                _uiState.update { state ->
                    // If the previously selected file was removed, deselect it.
                    val stillExists = files.any { it.id == state.selectedFileId }
                    state.copy(
                        audioFiles = files,
                        selectedFileId = if (stillExists) state.selectedFileId else null,
                    )
                }
            }
        }
    }

    /**
     * Imports an audio file from the given content [uri] into the app's
     * internal storage via the repository.
     */
    fun onAudioFilePicked(uri: Uri, fileName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                audioRepository.importAudioFile(uri, fileName)
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                Timber.e(e, "Failed to import audio file")
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }

    /**
     * Selects a file by [id].  Releases the current MediaPlayer if it was
     * playing a different file.
     */
    fun onSelectFile(id: Long) {
        if (_uiState.value.selectedFileId == id) return
        releaseMediaPlayer()
        _uiState.update {
            it.copy(
                selectedFileId = id,
                isPlaying = false,
                currentPosition = 0,
                duration = 0,
            )
        }
    }

    /** Deletes a file from the repository and deselects it if selected. */
    fun onDeleteFile(id: Long) {
        viewModelScope.launch {
            if (_uiState.value.selectedFileId == id) {
                releaseMediaPlayer()
                _uiState.update {
                    it.copy(selectedFileId = null, isPlaying = false, currentPosition = 0, duration = 0)
                }
            }
            try {
                audioRepository.deleteAudioFile(id)
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete audio file id=$id")
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    /**
     * Toggles between play and pause for the currently selected file.
     * Creates a [MediaPlayer] on first play; reuses it on subsequent calls.
     */
    fun onPlayPause() {
        val state = _uiState.value
        val selectedFile = state.audioFiles.find { it.id == state.selectedFileId } ?: return

        if (state.isPlaying) {
            // Pause
            mediaPlayer?.pause()
            positionUpdateJob?.cancel()
            _uiState.update { it.copy(isPlaying = false) }
        } else {
            val existing = mediaPlayer
            if (existing != null) {
                // Resume from paused position
                existing.start()
                _uiState.update { it.copy(isPlaying = true) }
                startPositionUpdates()
            } else {
                // First play – prepare asynchronously on IO
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val mp = MediaPlayer().apply {
                            setDataSource(selectedFile.filePath)
                            prepare() // blocking; run on IO
                        }
                        withContext(Dispatchers.Main) {
                            mediaPlayer = mp
                            mp.setOnCompletionListener {
                                positionUpdateJob?.cancel()
                                mediaPlayer = null
                                _uiState.update { it.copy(isPlaying = false, currentPosition = 0) }
                            }
                            mp.setOnErrorListener { _, what, extra ->
                                Timber.e("MediaPlayer error what=$what extra=$extra")
                                releaseMediaPlayer()
                                _uiState.update { it.copy(isPlaying = false, error = "Помилка відтворення ($what)") }
                                true
                            }
                            _uiState.update { it.copy(duration = mp.duration) }
                            mp.start()
                            _uiState.update { it.copy(isPlaying = true) }
                            startPositionUpdates()
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to prepare MediaPlayer for ${selectedFile.filePath}")
                        withContext(Dispatchers.Main) {
                            _uiState.update { it.copy(error = "Не вдалося відтворити файл: ${e.localizedMessage}") }
                        }
                    }
                }
            }
        }
    }

    /** Seeks the player to the given position in milliseconds. */
    fun onSeek(positionMs: Int) {
        mediaPlayer?.seekTo(positionMs)
        _uiState.update { it.copy(currentPosition = positionMs) }
    }

    /** Skips 10 seconds backward (minimum: 0). */
    fun onSkipBack() {
        val newPos = maxOf(0, (mediaPlayer?.currentPosition ?: 0) - 10_000)
        mediaPlayer?.seekTo(newPos)
        _uiState.update { it.copy(currentPosition = newPos) }
    }

    /** Skips 10 seconds forward (maximum: duration). */
    fun onSkipForward() {
        val duration = mediaPlayer?.duration ?: 0
        val newPos = minOf(duration, (mediaPlayer?.currentPosition ?: 0) + 10_000)
        mediaPlayer?.seekTo(newPos)
        _uiState.update { it.copy(currentPosition = newPos) }
    }

    /** Clears a previously shown error message. */
    fun onErrorDismissed() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Called on every keystroke in the audio delay input field.
     * Validates and persists immediately when valid.
     *
     * !! ВАЖЛИВО: Використовується AutoDialerService для затримки перед TX-аудіо !!
     */
    fun onAudioDelayChanged(text: String) {
        val trimmed = text.trim()
        val parsed = trimmed.toIntOrNull()
        val isValid = parsed != null && parsed >= 0
        _uiState.update {
            it.copy(
                audioDelayInput = text,
                audioDelayError = trimmed.isNotEmpty() && !isValid,
            )
        }
        if (isValid) {
            appPreferences.audioDelaySeconds = parsed!!
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /** Returns the file path of the currently selected file, or null. */
    fun selectedFilePath(): String? {
        val state = _uiState.value
        return state.audioFiles.find { it.id == state.selectedFileId }?.filePath
    }

    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = viewModelScope.launch {
            while (isActive) {
                val position = mediaPlayer?.currentPosition ?: break
                _uiState.update { it.copy(currentPosition = position) }
                delay(200L)
            }
        }
    }

    private fun releaseMediaPlayer() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
        mediaPlayer?.run {
            try {
                if (isPlaying) stop()
                release()
            } catch (e: Exception) {
                Timber.e(e, "Error releasing MediaPlayer")
            }
        }
        mediaPlayer = null
        _uiState.update { it.copy(isPlaying = false, currentPosition = 0, duration = 0) }
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    override fun onCleared() {
        super.onCleared()
        releaseMediaPlayer()
    }
}
