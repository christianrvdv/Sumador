// ui/sumador/SumadorScreen.kt
package cu.christianrvdv.sumador.ui.sumador

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import cu.christianrvdv.sumador.R
import cu.christianrvdv.sumador.data.database.CustomDenominationEntity
import cu.christianrvdv.sumador.ui.settings.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SumadorScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: SumadorViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel,
    onNavigateToHistory: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onNavigateToManageDenominations: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val settingsState by settingsViewModel.state.collectAsState()
    val denominations by viewModel.denominations.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveName by remember { mutableStateOf("") }

    var showUnsavedWarning by remember { mutableStateOf(false) }
    var shouldExitAfterSave by remember { mutableStateOf(false) }

    // Snackbar para feedback
    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    // Estados para exportación/importación manual
    var showImportConfirmDialog by remember { mutableStateOf(false) }
    var importUri by remember { mutableStateOf<Uri?>(null) }

    val isCoinMap = remember(denominations) {
        denominations.associate { it.denomination to it.isCoin }
    }

    LaunchedEffect(settingsState.autoSave) {
        viewModel.setAutoSave(settingsState.autoSave)
    }

    LaunchedEffect(settingsState.currencySymbol) {
        viewModel.setCurrency(settingsState.currencySymbol)
    }

    LaunchedEffect(settingsState.useCoins) {
        viewModel.setUseCoins(settingsState.useCoins)
    }

    BackHandler(enabled = true) {
        val hasBills = state.cantidades.any { (_, value) ->
            (value.toIntOrNull() ?: 0) > 0
        }
        if (!settingsState.autoSave && hasBills) {
            showUnsavedWarning = true
        } else {
            navController.popBackStack()
        }
    }

    val denominacionesOrdenadas = remember(denominations, settingsState.sortAscending) {
        if (settingsState.sortAscending) {
            denominations.sortedBy { it.denomination }
        } else {
            denominations.sortedByDescending { it.denomination }
        }
    }

    val totalFormateado = remember(state.total) {
        formatCurrency(state.total)
    }
    val totalBills = state.cantidades.values.sumOf { it.toIntOrNull() ?: 0 }

    val totalScale by animateFloatAsState(
        targetValue = if (state.total > 0) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "total_scale_bottom"
    )

    val isEmpty = state.cantidades.values.all { it.toIntOrNull() == 0 || it.isEmpty() }
    val hasBills = state.cantidades.values.any { it.toIntOrNull() ?: 0 > 0 }

    // ---- Launcher para Exportar ----
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            uri?.let {
                coroutineScope.launch {
                    val exportResult = settingsViewModel.exportDataToUri(context, it)
                    val messageRes = if (exportResult.isSuccess) {
                        R.string.export_success
                    } else {
                        val exception = exportResult.exceptionOrNull()
                        when {
                            exception?.message?.contains("abrir el archivo") == true -> R.string.export_error_open
                            exception?.message?.contains("permiso") == true -> R.string.export_error_permission
                            else -> R.string.export_error
                        }
                    }
                    snackbarMessage = context.getString(messageRes)
                }
            }
        }
    }

    // ---- Launcher para Importar ----
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            uri?.let {
                importUri = it
                showImportConfirmDialog = true
            }
        }
    }

    // Mostrar Snackbar cuando cambia el mensaje
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = if (data.visuals.message == context.getString(R.string.backup_success) ||
                        data.visuals.message == context.getString(R.string.export_success) ||
                        data.visuals.message == context.getString(R.string.import_success)
                    ) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.errorContainer
                )
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val appBarLogo = if (MaterialTheme.colorScheme.primary.luminance() < 0.5f) {
                            R.drawable.ic_logo_app_bar_dark
                        } else {
                            R.drawable.ic_logo_app_bar_light
                        }
                        Icon(
                            painter = painterResource(appBarLogo),
                            contentDescription = null,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = stringResource(R.string.history_title)
                        )
                    }
                    IconButton(onClick = onNavigateToAbout) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = stringResource(R.string.content_description_about)
                        )
                    }
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .scale(totalScale)
                        ) {
                            Text(
                                text = stringResource(R.string.total).uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.5.sp
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "$totalFormateado ",
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = settingsState.currencySymbol.symbol,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (totalBills > 0) {
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(
                                            R.string.total_bills_short,
                                            totalBills
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (hasBills) {
                                FilledTonalIconButton(
                                    onClick = {
                                        shareCurrentSum(
                                            context,
                                            state,
                                            settingsState.currencySymbol,
                                            denominations
                                        )
                                    },
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary,
                                        contentColor = MaterialTheme.colorScheme.onSecondary
                                    ),
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = stringResource(R.string.share_sum)
                                    )
                                }
                            }

                            if (hasBills) {
                                FilledTonalIconButton(
                                    onClick = { showSaveDialog = true },
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Save,
                                        contentDescription = stringResource(R.string.save_sum)
                                    )
                                }
                            }

                            FilledTonalIconButton(
                                onClick = {
                                    if (settingsState.confirmClear) {
                                        showResetDialog = true
                                    } else {
                                        viewModel.resetear()
                                    }
                                },
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.clear_all)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isEmpty) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.empty_state_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.empty_state_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            denominacionesOrdenadas.forEachIndexed { index, entity ->
                val enterTransition = fadeIn(animationSpec = tween(300, delayMillis = index * 40)) +
                        slideInVertically(
                            animationSpec = tween(
                                300,
                                delayMillis = index * 40
                            )
                        ) { it / 3 }

                AnimatedVisibility(
                    visible = true,
                    enter = enterTransition,
                    exit = fadeOut(animationSpec = tween(200)) +
                            slideOutVertically(animationSpec = tween(200)) { it / 3 }
                ) {
                    BillInputRow(
                        denomination = entity.denomination,
                        isCoin = entity.isCoin,
                        value = state.cantidades[entity.denomination] ?: "",
                        onValueChange = { newValue ->
                            viewModel.updateCantidad(
                                entity.denomination,
                                newValue
                            )
                        },
                        currencySymbol = settingsState.currencySymbol.symbol
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // === DIÁLOGOS ===

    // Confirmación de reinicio
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.clear_confirmation_title)) },
            text = { Text(stringResource(R.string.clear_confirmation_text)) },
            confirmButton = {
                TextButton(onClick = { viewModel.resetear(); showResetDialog = false }) {
                    Text(stringResource(R.string.clear_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Guardar suma
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = {
                showSaveDialog = false
                saveName = ""
                shouldExitAfterSave = false
            },
            title = { Text(stringResource(R.string.save_sum)) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = stringResource(R.string.enter_name_prompt),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = saveName,
                        onValueChange = { saveName = it },
                        placeholder = { Text(stringResource(R.string.save_sum_name_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.summary_label),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            val items = state.cantidades
                                .filter { it.value.toIntOrNull() ?: 0 > 0 }
                                .toList()
                                .sortedByDescending { (denom, _) -> denom }

                            if (items.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.no_bills_selected),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                items.forEach { (denom, cantidadStr) ->
                                    val cantidad = cantidadStr.toIntOrNull() ?: 0
                                    val isCoin = isCoinMap[denom] ?: (denom % 100 != 0)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${formatDenomination(denom, isCoin)} ${if (!isCoin && denom >= 100) settingsState.currencySymbol.symbol else ""}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "x $cantidad",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Divider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = stringResource(R.string.total_summary),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "$totalFormateado ${settingsState.currencySymbol.symbol}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val finalName = if (saveName.isNotBlank()) saveName else {
                            val dateFormat =
                                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                            dateFormat.format(Date())
                        }
                        val denominationsMap =
                            state.cantidades.mapValues { it.value.toIntOrNull() ?: 0 }
                        viewModel.saveCurrentSum(
                            name = finalName,
                            total = state.total,
                            denominationsMap = denominationsMap
                        )
                        showSaveDialog = false
                        saveName = ""
                        if (shouldExitAfterSave) {
                            shouldExitAfterSave = false
                            navController.popBackStack()
                        }
                    }
                ) {
                    Text(stringResource(R.string.save_sum))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSaveDialog = false
                        saveName = ""
                        shouldExitAfterSave = false
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Advertencia al salir sin auto-save
    if (showUnsavedWarning) {
        Dialog(
            onDismissRequest = { showUnsavedWarning = false }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.unsaved_warning_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.unsaved_warning_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            showUnsavedWarning = false
                            navController.popBackStack()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.unsaved_warning_exit))
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = { showUnsavedWarning = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        }
    }

    // Diálogo de ajustes (con los cambios)
    if (showSettingsDialog) {
        SettingsBottomSheet(
            settingsState = settingsState,
            onDismiss = { showSettingsDialog = false },
            onThemeChange = { theme -> coroutineScope.launch { settingsViewModel.updateTheme(theme) } },
            onCurrencyChange = { currency ->
                coroutineScope.launch {
                    settingsViewModel.updateCurrency(currency)
                }
            },
            onSortChange = { ascending ->
                coroutineScope.launch {
                    settingsViewModel.updateSortOrder(ascending)
                }
            },
            onAutoSaveChange = { enabled ->
                coroutineScope.launch {
                    settingsViewModel.updateAutoSave(enabled)
                }
            },
            onConfirmClearChange = { enabled ->
                coroutineScope.launch {
                    settingsViewModel.updateConfirmClear(enabled)
                }
            },
            onLanguageChange = { language ->
                coroutineScope.launch {
                    settingsViewModel.updateLanguage(language)
                }
            },
            onKeepScreenOnChange = { enabled ->
                coroutineScope.launch {
                    settingsViewModel.updateKeepScreenOn(enabled)
                }
            },
            onUseCoinsChange = { enabled ->
                coroutineScope.launch {
                    settingsViewModel.updateUseCoins(enabled)
                }
            },
            onAboutClick = onNavigateToAbout,
            onCheckForUpdates = onCheckForUpdates,
            onManageDenominations = onNavigateToManageDenominations,
            // onBackupRequest eliminado
            onExportRequest = {
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/octet-stream"
                    putExtra(
                        Intent.EXTRA_TITLE,
                        "sumador_backup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.sumadorbak"
                    )
                }
                exportLauncher.launch(intent)
            },
            onImportRequest = {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/octet-stream"
                    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/octet-stream", "application/json"))
                }
                importLauncher.launch(intent)
            }
        )
    }

    // ---- Diálogo de confirmación de importación (MEJORADO) ----
    if (showImportConfirmDialog && importUri != null) {
        AlertDialog(
            onDismissRequest = { showImportConfirmDialog = false; importUri = null },
            title = { Text(stringResource(R.string.import_confirm_title)) },
            text = { Text(stringResource(R.string.import_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            val importResult = settingsViewModel.importDataFromUri(context, importUri!!)
                            val messageRes = when {
                                importResult.isSuccess -> R.string.import_success
                                importResult.exceptionOrNull()?.message?.contains("vacío") == true -> R.string.import_error_empty
                                importResult.exceptionOrNull()?.message?.contains("tamaño insuficiente") == true -> R.string.import_error_invalid
                                importResult.exceptionOrNull()?.message?.contains("versión") == true -> R.string.import_error_version
                                else -> R.string.import_error
                            }
                            snackbarMessage = context.getString(messageRes)
                            showImportConfirmDialog = false
                            importUri = null
                        }
                    }
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirmDialog = false; importUri = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

// ===================== COMPONENTES UI =====================

@Composable
fun BillInputRow(
    denomination: Int,
    isCoin: Boolean,
    value: String,
    onValueChange: (String) -> Unit,
    currencySymbol: String
) {
    var textFieldValue by remember(value) { mutableStateOf(value) }
    LaunchedEffect(value) { if (textFieldValue != value) textFieldValue = value }

    val billCount = textFieldValue.toIntOrNull() ?: 0
    val scale by animateFloatAsState(
        targetValue = if (billCount > 0) 1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = if (billCount > 0) BorderStroke(
            1.5.dp,
            MaterialTheme.colorScheme.primary
        ) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Money, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = formatDenomination(denomination, isCoin),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!isCoin && denomination >= 100) {
                    Text(
                        text = currencySymbol,
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = {
                        if (billCount > 0) {
                            val newVal = (billCount - 1).toString()
                            textFieldValue = newVal
                            onValueChange(newVal)
                        }
                    },
                    enabled = billCount > 0,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = if (billCount > 0) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(50)
                        )
                ) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = stringResource(R.string.content_description_decrement),
                        tint = if (billCount > 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = { newText ->
                        if (newText.all { it.isDigit() } && newText.length <= 6) {
                            textFieldValue = newText
                            onValueChange(newText)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .width(88.dp)
                        .padding(horizontal = 2.dp),
                    placeholder = { Text("0") },
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                            alpha = 0.3f
                        ),
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                IconButton(
                    onClick = {
                        val newVal = (billCount + 1).toString()
                        textFieldValue = newVal
                        onValueChange(newVal)
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.content_description_increment),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

// ===================== FUNCIONES AUXILIARES =====================

fun formatCurrency(amount: Long): String {
    val whole = amount / 100
    val cents = amount % 100
    return if (cents == 0L) whole.toString() else "$whole.${String.format("%02d", cents)}"
}

fun formatDenomination(denom: Int, isCoin: Boolean): String {
    return if (isCoin) {
        "${denom}¢"
    } else {
        if (denom % 100 == 0) (denom / 100).toString()
        else "${denom / 100}.${String.format("%02d", denom % 100)}"
    }
}

fun shareCurrentSum(
    context: Context,
    state: SumadorState,
    currency: CurrencySymbol,
    denominations: List<CustomDenominationEntity>
) {
    val isCoinMap = denominations.associate { it.denomination to it.isCoin }

    val sb = StringBuilder()
    sb.append("💰 ${context.getString(R.string.total)}: ${formatCurrency(state.total)} ${currency.symbol}\n\n")
    sb.append("${context.getString(R.string.detail_label)}:\n")

    state.cantidades
        .filter { it.value.toIntOrNull() ?: 0 > 0 }
        .toList()
        .sortedByDescending { (denom, _) -> denom }
        .forEach { (denom, countStr) ->
            val count = countStr.toIntOrNull() ?: 0
            if (count > 0) {
                val value = denom * count
                val isCoin = isCoinMap[denom] ?: (denom % 100 != 0)
                sb.append(
                    "  ${formatDenomination(denom, isCoin)} x $count = ${formatCurrency(value.toLong())}\n"
                )
            }
        }

    val shareText = sb.toString()
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    try {
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_sum)))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.info_no_share_apps, Toast.LENGTH_SHORT).show()
    }
}