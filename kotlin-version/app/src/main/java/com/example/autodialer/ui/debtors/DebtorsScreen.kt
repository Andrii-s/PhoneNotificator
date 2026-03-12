package com.example.autodialer.ui.debtors

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.autodialer.R
import com.example.autodialer.domain.model.CallLog
import com.example.autodialer.domain.model.CallStatus
import com.example.autodialer.domain.model.Debtor
import com.example.autodialer.ui.theme.AutoDialerTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Debtors screen — lets the user choose a dial mode (server list, manual number,
 * or multi-line list), start/stop the auto-dialer, and review the call log.
 *
 * @param audioFilePath Path to the audio file selected in the Settings screen.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun DebtorsScreen(
    audioFilePath: String = "",
    viewModel: DebtorsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // ── Phone permissions ─────────────────────────────────────────────────────
    val phonePermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE,
        ),
    )

    // ── Error snackbar ────────────────────────────────────────────────────────
    LaunchedEffect(uiState.debtorsError) {
        uiState.debtorsError?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.onErrorDismissed()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.debtors_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // ── Mode tabs ─────────────────────────────────────────────────────
            val tabs = listOf(
                stringResource(R.string.debtors_tab_debtors),
                stringResource(R.string.debtors_tab_manual),
                stringResource(R.string.debtors_tab_list),
            )
            val selectedIndex = uiState.selectedMode.ordinal

            TabRow(selectedTabIndex = selectedIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedIndex == index,
                        onClick = { viewModel.onModeSelected(DialMode.entries[index]) },
                        text = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
            }

            // ── Mode content ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                when (uiState.selectedMode) {
                    DialMode.DEBTORS -> DebtorsTabContent(
                        uiState = uiState,
                        audioFilePath = audioFilePath,
                        phonePermissionsState = phonePermissionsState,
                        onFetchDebtors = viewModel::fetchDebtors,
                        onStartDialing = viewModel::startDialing,
                        onStopDialing = viewModel::stopDialing,
                    )
                    DialMode.MANUAL -> ManualTabContent(
                        uiState = uiState,
                        audioFilePath = audioFilePath,
                        phonePermissionsState = phonePermissionsState,
                        onNumberChanged = viewModel::onManualNumberChanged,
                        onStartDialing = viewModel::startDialing,
                        onStopDialing = viewModel::stopDialing,
                    )
                    DialMode.LIST -> ListTabContent(
                        uiState = uiState,
                        audioFilePath = audioFilePath,
                        phonePermissionsState = phonePermissionsState,
                        onNumbersChanged = viewModel::onListNumbersChanged,
                        onStartDialing = viewModel::startDialing,
                        onStopDialing = viewModel::stopDialing,
                    )
                }
            }

            // ── Call log section ──────────────────────────────────────────────
            HorizontalDivider()
            CallLogSection(callLogs = uiState.callLogs)
        }
    }
}

// =============================================================================
// Tab content composables
// =============================================================================

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun DebtorsTabContent(
    uiState: DebtorsViewModel.DebtorsUiState,
    audioFilePath: String,
    phonePermissionsState: MultiplePermissionsState,
    onFetchDebtors: () -> Unit,
    onStartDialing: (List<String>, String) -> Unit,
    onStopDialing: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedButton(
            onClick = onFetchDebtors,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoadingDebtors,
        ) {
            if (uiState.isLoadingDebtors) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .width(18.dp)
                        .height(18.dp),
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(stringResource(R.string.debtors_get_list))
        }

        if (uiState.debtors.isNotEmpty()) {
            // Table header
            DebtorTableHeader()
            HorizontalDivider()
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(uiState.debtors, key = { it.id }) { debtor ->
                    DebtorRow(index = uiState.debtors.indexOf(debtor) + 1, debtor = debtor)
                    HorizontalDivider()
                }
            }
        } else if (!uiState.isLoadingDebtors) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.debtors_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Spacer(Modifier.weight(1f))
        }

        // Dialing status indicator
        if (uiState.isDialing && uiState.dialingProgress.isNotEmpty()) {
            Text(
                text = uiState.dialingProgress,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        PermissionAwareDialButton(
            label = stringResource(if (uiState.isDialing) R.string.debtors_stop_calls else R.string.debtors_start_calls),
            enabled = uiState.debtors.isNotEmpty(),
            isDialing = uiState.isDialing,
            phonePermissionsState = phonePermissionsState,
            onStartDialing = {
                onStartDialing(uiState.debtors.map { it.phone }, audioFilePath)
            },
            onStopDialing = onStopDialing,
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun ManualTabContent(
    uiState: DebtorsViewModel.DebtorsUiState,
    audioFilePath: String,
    phonePermissionsState: MultiplePermissionsState,
    onNumberChanged: (String) -> Unit,
    onStartDialing: (List<String>, String) -> Unit,
    onStopDialing: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = uiState.manualNumber,
            onValueChange = onNumberChanged,
            label = { Text(stringResource(R.string.debtors_phone_number)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.weight(1f))

        if (uiState.isDialing && uiState.dialingProgress.isNotEmpty()) {
            Text(
                text = uiState.dialingProgress,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        PermissionAwareDialButton(
            label = stringResource(if (uiState.isDialing) R.string.debtors_stop_calls else R.string.debtors_start_call),
            enabled = uiState.manualNumber.isNotBlank(),
            isDialing = uiState.isDialing,
            phonePermissionsState = phonePermissionsState,
            onStartDialing = {
                onStartDialing(listOf(uiState.manualNumber), audioFilePath)
            },
            onStopDialing = onStopDialing,
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun ListTabContent(
    uiState: DebtorsViewModel.DebtorsUiState,
    audioFilePath: String,
    phonePermissionsState: MultiplePermissionsState,
    onNumbersChanged: (String) -> Unit,
    onStartDialing: (List<String>, String) -> Unit,
    onStopDialing: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = uiState.listNumbers,
            onValueChange = onNumbersChanged,
            label = { Text(stringResource(R.string.debtors_number_list)) },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            maxLines = Int.MAX_VALUE,
        )

        if (uiState.isDialing && uiState.dialingProgress.isNotEmpty()) {
            Text(
                text = uiState.dialingProgress,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        val parsedNumbers = uiState.listNumbers.lines().map { it.trim() }.filter { it.isNotBlank() }

        PermissionAwareDialButton(
            label = stringResource(if (uiState.isDialing) R.string.debtors_stop_calls else R.string.debtors_start_calls),
            enabled = parsedNumbers.isNotEmpty(),
            isDialing = uiState.isDialing,
            phonePermissionsState = phonePermissionsState,
            onStartDialing = {
                onStartDialing(parsedNumbers, audioFilePath)
            },
            onStopDialing = onStopDialing,
        )
    }
}

// =============================================================================
// Reusable sub-composables
// =============================================================================

/** Button that checks phone permissions before triggering the dial action. */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PermissionAwareDialButton(
    label: String,
    enabled: Boolean,
    isDialing: Boolean,
    phonePermissionsState: MultiplePermissionsState,
    onStartDialing: () -> Unit,
    onStopDialing: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val allGranted = phonePermissionsState.permissions.all { it.status.isGranted }

    Button(
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isDialing) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.primary,
        ),
        onClick = {
            if (isDialing) {
                onStopDialing()
            } else if (allGranted) {
                onStartDialing()
            } else {
                phonePermissionsState.launchMultiplePermissionRequest()
            }
        },
    ) {
        Text(label)
    }

    if (!allGranted) {
        Text(
            text = stringResource(R.string.permission_call_phone_rationale),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun DebtorTableHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Text(stringResource(R.string.debtors_col_num), modifier = Modifier.width(32.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
        Text(stringResource(R.string.debtors_col_name), modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
        Text(stringResource(R.string.debtors_col_phone), modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
        Text(stringResource(R.string.debtors_col_debt), modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End)
    }
}

@Composable
private fun DebtorRow(index: Int, debtor: Debtor, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = index.toString(),
            modifier = Modifier.width(32.dp),
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = debtor.name,
            modifier = Modifier.weight(2f),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = debtor.phone,
            modifier = Modifier.weight(2f),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = debtor.debt.toString(),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun CallLogSection(
    callLogs: List<CallLog>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.call_log_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        if (callLogs.isEmpty()) {
            Text(
                text = stringResource(R.string.call_log_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
            ) {
                items(callLogs.asReversed(), key = { it.id }) { log ->
                    CallLogRow(log = log)
                    HorizontalDivider()
                }
            }
        }
    }
}

private val timeFmt = SimpleDateFormat("HH:mm dd.MM", Locale.getDefault())

@Composable
private fun CallLogRow(log: CallLog, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = log.phone,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = timeFmt.format(Date(log.startTime)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = log.durationFormatted,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        val statusColor = when (log.status) {
            CallStatus.ANSWERED -> MaterialTheme.colorScheme.primary
            CallStatus.NO_ANSWER -> MaterialTheme.colorScheme.onSurfaceVariant
            CallStatus.FAILED -> MaterialTheme.colorScheme.error
        }
        Text(
            text = log.status.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = statusColor,
        )
    }
}

// =============================================================================
// Preview
// =============================================================================

@Preview(showBackground = true)
@Composable
private fun DebtorsScreenPreview() {
    AutoDialerTheme {
        DebtorsScreen(onNavigateBack = {})
    }
}
