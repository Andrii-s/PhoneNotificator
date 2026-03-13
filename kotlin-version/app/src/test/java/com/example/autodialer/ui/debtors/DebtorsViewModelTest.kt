package com.example.autodialer.ui.debtors

import android.content.Context
import app.cash.turbine.test
import com.example.autodialer.domain.model.CallLog
import com.example.autodialer.domain.model.CallStatus
import com.example.autodialer.domain.model.Debtor
import com.example.autodialer.domain.repository.CallRepository
import com.example.autodialer.domain.repository.DebtorRepository
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
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Unit tests for [DebtorsViewModel].
 *
 * Strategy
 * --------
 * • [StandardTestDispatcher] is installed as [Dispatchers.Main] so that
 *   coroutines launched inside [androidx.lifecycle.ViewModel.viewModelScope]
 *   run only when we explicitly call [advanceUntilIdle].  This gives us
 *   deterministic control over state transitions.
 * • The [CallRepository.getCallLogs] stub returns a simple [flowOf] by default
 *   so that the [DebtorsViewModel.observeCallLogs] init-block does not hang.
 * • Turbine's [Flow.test] extension is used for tests that assert a specific
 *   *sequence* of UI-state emissions (e.g. loading → success, loading → error).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DebtorsViewModelTest {

    // -------------------------------------------------------------------------
    // Test infrastructure
    // -------------------------------------------------------------------------

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var debtorRepository: DebtorRepository
    private lateinit var callRepository: CallRepository
    private lateinit var context: Context

    private lateinit var viewModel: DebtorsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        debtorRepository = mock()
        callRepository   = mock()
        context          = mock()

        // Provide a non-hanging stub for the init-block's observeCallLogs()
        whenever(callRepository.getCallLogs()).thenReturn(flowOf(emptyList()))

        viewModel = DebtorsViewModel(debtorRepository, callRepository, context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // -------------------------------------------------------------------------
    // Initial state
    // -------------------------------------------------------------------------

    @Test
    fun `initial state has empty debtors, no loading, no error, DEBTORS mode`() {
        val state = viewModel.uiState.value
        assertTrue(state.debtors.isEmpty())
        assertFalse(state.isLoadingDebtors)
        assertNull(state.debtorsError)
        assertEquals(DialMode.DEBTORS, state.selectedMode)
        assertEquals("", state.manualNumber)
        assertEquals("", state.listNumbers)
    }

    // -------------------------------------------------------------------------
    // fetchDebtors – direct state value assertions
    // -------------------------------------------------------------------------

    @Test
    fun `fetchDebtors success sets debtors list and clears loading`() = runTest(testDispatcher) {
        val expected = listOf(
            Debtor(1L, "John Doe",  "+380991234567", 1_500.0),
            Debtor(2L, "Jane Doe",  "+380997654321", 2_500.0),
        )
        whenever(debtorRepository.fetchDebtors()).thenReturn(expected)

        viewModel.fetchDebtors()
        advanceUntilIdle()

        with(viewModel.uiState.value) {
            assertEquals(expected, debtors)
            assertFalse(isLoadingDebtors)
            assertNull(debtorsError)
        }
    }

    @Test
    fun `fetchDebtors error sets error message and clears loading`() = runTest(testDispatcher) {
        whenever(debtorRepository.fetchDebtors())
            .thenThrow(RuntimeException("Network error"))

        viewModel.fetchDebtors()
        advanceUntilIdle()

        with(viewModel.uiState.value) {
            assertTrue(debtors.isEmpty())
            assertFalse(isLoadingDebtors)
            assertEquals("Network error", debtorsError)
        }
    }

    @Test
    fun `fetchDebtors replaces previous debtors list on second call`() = runTest(testDispatcher) {
        val firstBatch  = listOf(Debtor(1L, "Alice", "+380991111111", 100.0))
        val secondBatch = listOf(Debtor(2L, "Bob",   "+380992222222", 200.0))

        whenever(debtorRepository.fetchDebtors())
            .thenReturn(firstBatch)
            .thenReturn(secondBatch)

        viewModel.fetchDebtors()
        advanceUntilIdle()
        assertEquals(firstBatch, viewModel.uiState.value.debtors)

        viewModel.fetchDebtors()
        advanceUntilIdle()
        assertEquals(secondBatch, viewModel.uiState.value.debtors)
    }

    // -------------------------------------------------------------------------
    // fetchDebtors – Turbine flow-of-states assertions
    // -------------------------------------------------------------------------

    @Test
    fun `fetchDebtors emits loading state then success state`() = runTest(testDispatcher) {
        val outerScope = this
        val expected   = listOf(Debtor(1L, "Alice", "+380991111111", 3_000.0))
        whenever(debtorRepository.fetchDebtors()).thenReturn(expected)

        viewModel.uiState.test {
            // Consume the initial state emitted by the StateFlow on subscription.
            val initial = awaitItem()
            assertFalse(initial.isLoadingDebtors)
            assertTrue(initial.debtors.isEmpty())

            // Trigger + advance so all coroutines inside fetchDebtors() complete.
            viewModel.fetchDebtors()
            outerScope.advanceUntilIdle()

            // --- loading state ---
            val loading = awaitItem()
            assertTrue(loading.isLoadingDebtors)
            assertNull(loading.debtorsError)

            // --- success state ---
            val success = awaitItem()
            assertFalse(success.isLoadingDebtors)
            assertEquals(expected, success.debtors)
            assertNull(success.debtorsError)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `fetchDebtors emits loading state then error state`() = runTest(testDispatcher) {
        val outerScope = this
        whenever(debtorRepository.fetchDebtors())
            .thenThrow(RuntimeException("Timeout"))

        viewModel.uiState.test {
            awaitItem() // initial state

            viewModel.fetchDebtors()
            outerScope.advanceUntilIdle()

            // --- loading state ---
            val loading = awaitItem()
            assertTrue(loading.isLoadingDebtors)

            // --- error state ---
            val error = awaitItem()
            assertFalse(error.isLoadingDebtors)
            assertEquals("Timeout", error.debtorsError)
            assertTrue(error.debtors.isEmpty())

            cancelAndIgnoreRemainingEvents()
        }
    }

    // -------------------------------------------------------------------------
    // onModeSelected
    // -------------------------------------------------------------------------

    @Test
    fun `onModeSelected MANUAL updates selectedMode`() {
        viewModel.onModeSelected(DialMode.MANUAL)
        assertEquals(DialMode.MANUAL, viewModel.uiState.value.selectedMode)
    }

    @Test
    fun `onModeSelected LIST updates selectedMode`() {
        viewModel.onModeSelected(DialMode.LIST)
        assertEquals(DialMode.LIST, viewModel.uiState.value.selectedMode)
    }

    @Test
    fun `onModeSelected DEBTORS resets to default mode`() {
        viewModel.onModeSelected(DialMode.MANUAL)
        viewModel.onModeSelected(DialMode.DEBTORS)
        assertEquals(DialMode.DEBTORS, viewModel.uiState.value.selectedMode)
    }

    @Test
    fun `onModeSelected clears existing debtorsError`() = runTest(testDispatcher) {
        whenever(debtorRepository.fetchDebtors())
            .thenThrow(RuntimeException("Error"))
        viewModel.fetchDebtors()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.debtorsError)

        viewModel.onModeSelected(DialMode.MANUAL)

        assertNull(viewModel.uiState.value.debtorsError)
    }

    // -------------------------------------------------------------------------
    // onManualNumberChanged
    // -------------------------------------------------------------------------

    @Test
    fun `onManualNumberChanged updates manualNumber`() {
        viewModel.onManualNumberChanged("+380991234567")
        assertEquals("+380991234567", viewModel.uiState.value.manualNumber)
    }

    @Test
    fun `onManualNumberChanged handles empty string`() {
        viewModel.onManualNumberChanged("+380991234567")
        viewModel.onManualNumberChanged("")
        assertEquals("", viewModel.uiState.value.manualNumber)
    }

    @Test
    fun `onManualNumberChanged overwrites previous value`() {
        viewModel.onManualNumberChanged("+380991111111")
        viewModel.onManualNumberChanged("+380992222222")
        assertEquals("+380992222222", viewModel.uiState.value.manualNumber)
    }

    // -------------------------------------------------------------------------
    // onListNumbersChanged
    // -------------------------------------------------------------------------

    @Test
    fun `onListNumbersChanged updates listNumbers`() {
        val numbers = "+380991234567\n+380997654321\n+380993333333"
        viewModel.onListNumbersChanged(numbers)
        assertEquals(numbers, viewModel.uiState.value.listNumbers)
    }

    @Test
    fun `onListNumbersChanged handles empty input`() {
        viewModel.onListNumbersChanged("+380991234567")
        viewModel.onListNumbersChanged("")
        assertEquals("", viewModel.uiState.value.listNumbers)
    }

    // -------------------------------------------------------------------------
    // onErrorDismissed
    // -------------------------------------------------------------------------

    @Test
    fun `onErrorDismissed clears debtorsError`() = runTest(testDispatcher) {
        whenever(debtorRepository.fetchDebtors())
            .thenThrow(RuntimeException("Some error"))
        viewModel.fetchDebtors()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.debtorsError)

        viewModel.onErrorDismissed()

        assertNull(viewModel.uiState.value.debtorsError)
    }

    @Test
    fun `onErrorDismissed is idempotent when no error is set`() {
        assertNull(viewModel.uiState.value.debtorsError)
        viewModel.onErrorDismissed() // should not throw
        assertNull(viewModel.uiState.value.debtorsError)
    }

    // -------------------------------------------------------------------------
    // Call-log observation
    // -------------------------------------------------------------------------

    @Test
    fun `callLogs state is updated when repository emits new values`() = runTest(testDispatcher) {
        val callLogs = listOf(
            CallLog(
                id              = 1L,
                phone           = "+380991234567",
                startTime       = 1_000L,
                endTime         = 61_000L,
                durationSeconds = 60L,
                status          = CallStatus.ANSWERED,
            ),
        )

        // Wire a live MutableStateFlow so we can push updates after construction.
        val logsFlow = MutableStateFlow<List<CallLog>>(emptyList())
        whenever(callRepository.getCallLogs()).thenReturn(logsFlow)

        val vm = DebtorsViewModel(debtorRepository, callRepository, context)
        advanceUntilIdle()

        assertEquals(emptyList<CallLog>(), vm.uiState.value.callLogs)

        logsFlow.value = callLogs
        advanceUntilIdle()

        assertEquals(callLogs, vm.uiState.value.callLogs)
    }

    @Test
    fun `callLogs state reflects multiple consecutive emissions`() = runTest(testDispatcher) {
        val batch1 = listOf(
            CallLog(1L, "+380991111111", 0L, 30_000L, 30L, CallStatus.ANSWERED),
        )
        val batch2 = batch1 + CallLog(2L, "+380992222222", 0L, 60_000L, 60L, CallStatus.NO_ANSWER)

        val logsFlow = MutableStateFlow<List<CallLog>>(emptyList())
        whenever(callRepository.getCallLogs()).thenReturn(logsFlow)

        val vm = DebtorsViewModel(debtorRepository, callRepository, context)
        advanceUntilIdle()

        logsFlow.value = batch1
        advanceUntilIdle()
        assertEquals(batch1, vm.uiState.value.callLogs)

        logsFlow.value = batch2
        advanceUntilIdle()
        assertEquals(batch2, vm.uiState.value.callLogs)
    }
}
