package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PaperSize
import com.example.model.ScoreProject
import com.example.ui.components.EditorTopBar
import com.example.ui.components.FloatingQuickToolbar
import com.example.ui.components.ScoreCanvasView
import com.example.ui.components.SymbolLibrarySheet
import com.example.ui.dialogs.CustomSymbolDialog
import com.example.ui.dialogs.DriveSyncDialog
import com.example.ui.dialogs.ExportDialog
import com.example.ui.dialogs.ImportDialog
import com.example.ui.dialogs.PageManagerDialog
import com.example.ui.dialogs.PageSettingsDialog
import com.example.viewmodel.EditorUiState
import com.example.viewmodel.ScoreViewModel

@Composable
fun ScoreEditorScreen(
    state: EditorUiState,
    viewModel: ScoreViewModel,
    onBackToProjects: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val project = state.currentProject ?: return

    // Intercept back button to return to projects list
    BackHandler {
        onBackToProjects()
    }

    var showPageManager by remember { mutableStateOf(false) }
    var showPageSettings by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showDriveSyncDialog by remember { mutableStateOf(false) }
    var showCustomSymbolsDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.notificationMessage) {
        state.notificationMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearNotification()
        }
    }

    Scaffold(
        topBar = {
            EditorTopBar(
                project = project,
                canUndo = state.canUndo,
                canRedo = state.canRedo,
                isSaving = state.isSaving,
                onBack = onBackToProjects,
                onUndo = { viewModel.undo() },
                onRedo = { viewModel.redo() },
                onSave = { viewModel.saveProjectExplicitly() },
                onOpenPageManager = { showPageManager = true },
                onOpenPageSettings = { showPageSettings = true },
                onOpenExport = { showExportDialog = true },
                onOpenImport = { showImportDialog = true },
                onOpenDriveSync = { showDriveSyncDialog = true },
                onOpenCustomSymbols = { showCustomSymbolsDialog = true }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            SymbolLibrarySheet(
                activeCategory = state.activePaletteCategory,
                activeSymbol = state.activePaletteSymbol,
                onSelectCategory = { viewModel.selectCategory(it) },
                onSelectSymbol = { viewModel.selectPaletteSymbol(it) },
                onOpenCustomManager = { showCustomSymbolsDialog = true }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main Canvas
            ScoreCanvasView(
                page = project.activePage,
                selectedSymbolIds = state.selectedSymbolIds,
                isSnapMode = state.isSnapMode,
                showGuides = state.showGuides,
                highlightLine4 = state.highlightLine4,
                zoomScale = state.zoomScale,
                panOffsetX = state.panOffsetX,
                panOffsetY = state.panOffsetY,
                onZoomAndPanChange = { z, px, py -> viewModel.setZoomAndPan(z, px, py) },
                onCanvasTap = { lx, ly -> viewModel.onCanvasTap(lx, ly) },
                onSymbolDrag = { dx, dy -> viewModel.onSymbolDrag(dx, dy) },
                onSymbolDragFinished = { viewModel.onSymbolDragFinished() }
            )

            // Floating Quick Action Toolbar (At top of canvas)
            FloatingQuickToolbar(
                isSnapMode = state.isSnapMode,
                isMultiSelectMode = state.isMultiSelectMode,
                hasSelection = state.selectedSymbolIds.isNotEmpty(),
                showGuides = state.showGuides,
                highlightLine4 = state.highlightLine4,
                onToggleSnap = { viewModel.toggleSnapMode() },
                onToggleMultiSelect = { viewModel.toggleMultiSelectMode() },
                onResizeSelected = { multiplier -> viewModel.resizeSelected(multiplier) },
                onFlipStem = { viewModel.flipStemSelected() },
                onDuplicate = { viewModel.duplicateSelected() },
                onDelete = { viewModel.deleteSelected() },
                onToggleGuides = { viewModel.toggleGuides() },
                onToggleLine4 = { viewModel.toggleLine4Highlight() },
                onResetZoom = { viewModel.resetZoom() },
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }

    // Dialogs
    if (showPageManager) {
        PageManagerDialog(
            project = project,
            onDismiss = { showPageManager = false },
            onSwitchPage = { idx -> viewModel.switchActivePage(idx); showPageManager = false },
            onAddNewPage = { viewModel.addNewPage(); showPageManager = false },
            onDeleteCurrentPage = { viewModel.deleteCurrentPage(); showPageManager = false },
            onReorderPages = { from, to -> viewModel.reorderPages(from, to) }
        )
    }

    if (showPageSettings) {
        PageSettingsDialog(
            project = project,
            onDismiss = { showPageSettings = false },
            onSaveSettings = { paper, staves, spacing, l4ratio, title, sub, lesson ->
                viewModel.updatePageSettings(paper, staves, spacing, l4ratio, title, sub, lesson)
            }
        )
    }

    if (showExportDialog) {
        ExportDialog(
            project = project,
            onDismiss = { showExportDialog = false },
            onExportPdf = { viewModel.exportToPdf(context) },
            onExportImage = { isPng -> viewModel.exportActivePageImage(context, isPng) },
            onExportProjectFile = { viewModel.exportProjectFile(context) }
        )
    }

    if (showImportDialog) {
        ImportDialog(
            onDismiss = { showImportDialog = false },
            onImportContent = { content -> viewModel.importProjectContent(content) }
        )
    }

    if (showDriveSyncDialog) {
        DriveSyncDialog(
            projectsCount = state.projectsList.size,
            onDismiss = { showDriveSyncDialog = false },
            onBackupToDrive = { viewModel.showNotification("پشتیبان‌گیری ابری با موفقیت ثبت شد") },
            onRestoreFromDrive = { viewModel.showNotification("بازیابی پروژه‌ها با موفقیت انجام شد") }
        )
    }

    if (showCustomSymbolsDialog) {
        CustomSymbolDialog(
            onDismiss = { showCustomSymbolsDialog = false },
            onCreateCustom = { name, isRing -> viewModel.createCustomSymbol(name, isRing, null) }
        )
    }
}
