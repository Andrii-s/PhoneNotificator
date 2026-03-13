package com.example.autodialer.ui.settings

import android.content.Context
import android.net.Uri
import app.cash.turbine.test
import com.example.autodialer.domain.model.AudioFile
import com.example.autodialer.domain.repository.AudioRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Unit tests for [SettingsViewModel].
 *
 * Strategy
 * --------
 * • [StandardTestDispatcher] is installed as [Dispatchers.Main] so that coroutines
 *   launched inside [androidx.lifecycle.ViewModel.viewModelScope] run only when we
 *   explicitly call [advanceUntilIdle].
 * • [AudioRepository.getAllAudioFiles] is stubbed to return [flowOf] with an empty
 *   list by default, preventing the init-block's [SettingsViewModel.loadAudioFiles]
 *   coroutine from hanging.
 * • Android's [Uri] and [Context] are Mockito mocks; no Android runtime is required
 *   because `testOptions.unitTests.isReturnDefaultValues = true` is set in the module's
 *   build.gradle.kts, and the mocked repository never invokes Android-specific code.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    // -------------------------------------------------------------------------
    // Test infrastructure
    // -------------------------------------------------------------------------

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var audioRepository: AudioRepository
    private lateinit var context: Context

    private lateinit var viewModel: SettingsViewModel

    /** Minimal audio-file factory for reuse across tests. */
    private fun audioFile(id: Long, name: String = "file_$id.mp3") =
        AudioFile(id = id, fileName = name, filePath = "/storage/$name", addedAt = id * 1_000L)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        audioRepository = mock()
        context         = mock()

        // Default stub: repository emits an empty list so loadAudioFiles() does not hang.
        whenever(audioRepository.getAllAudioFiles()).thenReturn(flowOf(emptyList()))

        viewModel = SettingsViewModel(audioRepository, context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // -------------------------------------------------------------------------
    // Initial state
    // -------------------------------------------------------------------------

    @Test
    fun `initial state has empty audioFiles list`() = runTest(testDispatcher) {
        advanceUntilIdle() // let loadAudioFiles() coroutine complete
        assertTrue(viewModel.uiState.value.audioFiles.isEmpty())
    }

    @Test
    fun `initial state has null selectedFileId`() = runTest(testDispatcher) {
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.selectedFileId)
    }

    @Test
    fun `initial state has no playback in progress`() = runTest(testDispatcher) {
        advanceUntilIdle()
        with(viewModel.uiState.value) {
            assertFalse(isPlaying)
            assertEquals(0, currentPosition)
            assertEquals(0, duration)
        }
    }

    @Test
    fun `initial state has no error`() = runTest(testDispatcher) {
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `initial state is not loading`() = runTest(testDispatcher) {
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
    }

    // -------------------------------------------------------------------------
    // loadAudioFiles – repository emits files
    // -------------------------------------------------------------------------

    @Test
    fun `loadAudioFiles populates audioFiles from repository`() = runTest(testDispatcher) {
        val files = listOf(audioFile(1L), audioFile(2L))
        whenever(audioRepository.getAllAudioFiles()).thenReturn(flowOf(files))

        // Recreate so the new stub is used in init.
        val vm = SettingsViewModel(audioRepository, context)
        advanceUntilIdle()

        assertEquals(files, vm.uiState.value.audioFiles)
    }

    @Test
    fun `loadAudioFiles reflects live updates from repository flow`() = runTest(testDispatcher) {
        val filesFlow = MutableStateFlow<List<AudioFile>>(emptyList())
        whenever(audioRepository.getAllAudioFiles()).thenReturn(filesFlow)

        val vm = SettingsViewModel(audioRepository, context)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.audioFiles.isEmpty())

        val newFiles = listOf(audioFile(1L))
        filesFlow.value = newFiles
        advanceUntilIdle()

        assertEquals(newFiles, vm.uiState.value.audioFiles)
    }

    @Test
    fun `loadAudioFiles deselects file when it is removed from the list`() = runTest(testDispatcher) {
        val file      = audioFile(1L)
        val filesFlow = MutableStateFlow(listOf(file))
        whenever(audioRepository.getAllAudioFiles()).thenReturn(filesFlow)

        val vm = SettingsViewModel(audioRepository, context)
        advanceUntilIdle()

        vm.onSelectFile(1L)
        assertEquals(1L, vm.uiState.value.selectedFileId)

        // Remove the file from the flow → selectedFileId must be cleared.
        filesFlow.value = emptyList()
        advanceUntilIdle()

        assertNull(vm.uiState.value.selectedFileId)
    }

    // -------------------------------------------------------------------------
    // onSelectFile
    // -------------------------------------------------------------------------

    @Test
    fun `onSelectFile updates selectedFileId`() {
        viewModel.onSelectFile(42L)
        assertEquals(42L, viewModel.uiState.value.selectedFileId)
    }

    @Test
    fun `onSelectFile resets playback fields when switching to a new file`() {
        viewModel.onSelectFile(1L)
        viewModel.onSelectFile(2L)

        with(viewModel.uiState.value) {
            assertEquals(2L, selectedFileId)
            assertFalse(isPlaying)
            assertEquals(0, currentPosition)
            assertEquals(0, duration)
        }
    }

    @Test
    fun `onSelectFile with same id does not change state`() {
        viewModel.onSelectFile(1L)
        val stateAfterFirst = viewModel.uiState.value

        // Calling with the same id must be a no-op (early return in ViewModel).
        viewModel.onSelectFile(1L)

        assertEquals(stateAfterFirst, viewModel.uiState.value)
    }

    @Test
    fun `onSelectFile with same id emits no extra state via Turbine`() = runTest(testDispatcher) {
        val outerScope = this

        viewModel.uiState.test {
            awaitItem() // initial state

            viewModel.onSelectFile(5L)
            outerScope.advanceUntilIdle()

            val afterSelect = awaitItem()
            assertEquals(5L, afterSelect.selectedFileId)

            // Second call with identical id – StateFlow must NOT emit a new value.
            viewModel.onSelectFile(5L)
            outerScope.advanceUntilIdle()

            expectNoEvents()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onSelectFile sequential selections update selectedFileId each time`() {
        viewModel.onSelectFile(1L)
        assertEquals(1L, viewModel.uiState.value.selectedFileId)

        viewModel.onSelectFile(2L)
        assertEquals(2L, viewModel.uiState.value.selectedFileId)

        viewModel.onSelectFile(3L)
        assertEquals(3L, viewModel.uiState.value.selectedFileId)
    }

    // -------------------------------------------------------------------------
    // selectedFilePath
    // -------------------------------------------------------------------------

    @Test
    fun `selectedFilePath returns null when no file is selected`() = runTest(testDispatcher) {
        advanceUntilIdle()
        assertNull(viewModel.selectedFilePath())
    }

    @Test
    fun `selectedFilePath returns path of selected file`() = runTest(testDispatcher) {
        val files = listOf(audioFile(10L, "intro.mp3"))
        whenever(audioRepository.getAllAudioFiles()).thenReturn(flowOf(files))

        val vm = SettingsViewModel(audioRepository, context)
        advanceUntilIdle()

        vm.onSelectFile(10L)

        assertEquals("/storage/intro.mp3", vm.selectedFilePath())
    }

    // -------------------------------------------------------------------------
    // onAudioFilePicked (import success / failure)
    // -------------------------------------------------------------------------

    @Test
    fun `onAudioFilePicked sets isLoading true then false on success`() = runTest(testDispatcher) {
        val outerScope = this
        val uri        = mock<Uri>()
        val imported   = audioFile(99L, "new.mp3")

        whenever(audioRepository.importAudioFile(uri, "new.mp3")).thenReturn(imported)

        viewModel.uiState.test {
            awaitItem() // initial state

            viewModel.onAudioFilePicked(uri, "new.mp3")
            outerScope.advanceUntilIdle()

            // isLoading = true
            val loading = awaitItem()
            assertTrue(loading.isLoading)
            assertNull(loading.error)

            // isLoading = false after success
            val done = awaitItem()
            assertFalse(done.isLoading)
            assertNull(done.error)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onAudioFilePicked sets error when import throws`() = runTest(testDispatcher) {
        val uri = mock<Uri>()
        whenever(audioRepository.importAudioFile(any(), any()))
            .thenThrow(RuntimeException("Import failed"))

        viewModel.onAudioFilePicked(uri, "broken.mp3")
        advanceUntilIdle()

        with(viewModel.uiState.value) {
            assertFalse(isLoading)
            assertEquals("Import failed", error)
        }
    }

    // -------------------------------------------------------------------------
    // onErrorDismissed
    // -------------------------------------------------------------------------

    @Test
    fun `onErrorDismissed clears error`() = runTest(testDispatcher) {
        val uri = mock<Uri>()
        whenever(audioRepository.importAudioFile(any(), any()))
            .thenThrow(RuntimeException("Some import error"))

        viewModel.onAudioFilePicked(uri, "broken.mp3")
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)

        viewModel.onErrorDismissed()

        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `onErrorDismissed is idempotent when no error is set`() {
        assertNull(viewModel.uiState.value.error)
        viewModel.onErrorDismissed() // must not throw
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `onErrorDismissed only clears error, does not affect other fields`() = runTest(testDispatcher) {
        val uri = mock<Uri>()
        whenever(audioRepository.importAudioFile(any(), any()))
            .thenThrow(RuntimeException("Error"))

        viewModel.onSelectFile(7L)
        viewModel.onAudioFilePicked(uri, "bad.mp3")
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        assertEquals(7L, viewModel.uiState.value.selectedFileId)

        viewModel.onErrorDismissed()

        assertNull(viewModel.uiState.value.error)
        // selectedFileId must remain unchanged
        assertEquals(7L, viewModel.uiState.value.selectedFileId)
    }

    // -------------------------------------------------------------------------
    // onDeleteFile
    // -------------------------------------------------------------------------

    @Test
    fun `onDeleteFile deselects the file when it is the currently selected one`() =
        runTest(testDispatcher) {
            viewModel.onSelectFile(1L)
            assertEquals(1L, viewModel.uiState.value.selectedFileId)

            viewModel.onDeleteFile(1L)
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.selectedFileId)
        }

    @Test
    fun `onDeleteFile leaves selectedFileId unchanged when deleting a different file`() =
        runTest(testDispatcher) {
            viewModel.onSelectFile(1L)

            viewModel.onDeleteFile(2L)
            advanceUntilIdle()

            assertEquals(1L, viewModel.uiState.value.selectedFileId)
        }

    @Test
    fun `onDeleteFile sets error when repository throws`() = runTest(testDispatcher) {
        whenever(audioRepository.deleteAudioFile(any()))
            .thenThrow(RuntimeException("Delete failed"))

        viewModel.onDeleteFile(99L)
        advanceUntilIdle()

        assertEquals("Delete failed", viewModel.uiState.value.error)
    }
}
