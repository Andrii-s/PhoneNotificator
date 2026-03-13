package com.example.autodialer.ui.debtors

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.autodialer.domain.model.CallLog
import com.example.autodialer.domain.model.Debtor
import com.example.autodialer.domain.repository.CallRepository
import com.example.autodialer.domain.repository.DebtorRepository
import com.example.autodialer.service.AutoDialerService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/** Dial mode selected by the user in the TabRow. */
enum class DialMode { DEBTORS, MANUAL, LIST }

/**
 * ViewModel for the Debtors screen.
 *
 * Handles three dialing modes (server-fetched debtors, manual number entry,
 * and a multi-line list of numbers), observes call logs, and starts/stops
 * the [AutoDialerService].
 */
@HiltViewModel
class DebtorsViewModel @Inject constructor(
    private val debtorRepository: DebtorRepository,
    private val callRepository: CallRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    // -------------------------------------------------------------------------
    // UI State
    // -------------------------------------------------------------------------

    data class DebtorsUiState(
        val selectedMode: DialMode = DialMode.DEBTORS,
        val debtors: List<Debtor> = emptyList(),
        val isLoadingDebtors: Boolean = false,
        val debtorsError: String? = null,
        val manualNumber: String = "",
        val listNumbers: String = "",
        val callLogs: List<CallLog> = emptyList(),
        val isDialing: Boolean = false,
        /** Human-readable progress text shown in the UI while dialing. */
        val dialingProgress: String = "",
    )

    private val _uiState = MutableStateFlow(DebtorsUiState())
    val uiState: StateFlow<DebtorsUiState> = _uiState.asStateFlow()

    // -------------------------------------------------------------------------
    // Init
    // -------------------------------------------------------------------------

    init {
        observeCallLogs()
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    fun onModeSelected(mode: DialMode) {
        _uiState.update { it.copy(selectedMode = mode, debtorsError = null) }
    }

    /** Fetches the debtor list from the remote API. */
    fun fetchDebtors() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDebtors = true, debtorsError = null) }
            try {
                val debtors = debtorRepository.fetchDebtors()
                _uiState.update { it.copy(debtors = debtors, isLoadingDebtors = false) }
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch debtors")
                _uiState.update {
                    it.copy(
                        isLoadingDebtors = false,
                        debtorsError = e.localizedMessage ?: "Невідома помилка",
                    )
                }
            }
        }
    }

    fun onManualNumberChanged(number: String) {
        _uiState.update { it.copy(manualNumber = number) }
    }

    fun onListNumbersChanged(numbers: String) {
        _uiState.update { it.copy(listNumbers = numbers) }
    }

    /**
     * Sends [ACTION_START] to [AutoDialerService] with the given [numbers]
     * and [audioFilePath], then marks the session as dialing.
     */
    fun startDialing(numbers: List<String>, audioFilePath: String) {
        if (numbers.isEmpty()) {
            Timber.w("startDialing called with empty number list")
            return
        }
        Timber.i("Starting dialing: ${numbers.size} numbers, audio=$audioFilePath")

        val intent = Intent(context, AutoDialerService::class.java).apply {
            action = AutoDialerService.ACTION_START
            putStringArrayListExtra(AutoDialerService.EXTRA_NUMBERS, ArrayList(numbers))
            putExtra(AutoDialerService.EXTRA_AUDIO_FILE_PATH, audioFilePath)
        }
        context.startForegroundService(intent)

        _uiState.update {
            it.copy(
                isDialing = true,
                dialingProgress = "Розпочинаємо обдзвін ${numbers.size} номерів…",
            )
        }
    }

    /** Sends [ACTION_STOP] to [AutoDialerService] and resets dialing state. */
    fun stopDialing() {
        val intent = Intent(context, AutoDialerService::class.java).apply {
            action = AutoDialerService.ACTION_STOP
        }
        context.startService(intent)
        _uiState.update { it.copy(isDialing = false, dialingProgress = "") }
    }

    /** Clears a previously shown error. */
    fun onErrorDismissed() {
        _uiState.update { it.copy(debtorsError = null) }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private fun observeCallLogs() {
        viewModelScope.launch {
            callRepository.getCallLogs().collect { logs ->
                _uiState.update { it.copy(callLogs = logs) }
            }
        }
    }
}
