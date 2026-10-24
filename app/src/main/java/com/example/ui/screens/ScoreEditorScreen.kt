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
import com.example.ui.dialogs.CategoryManagerDialog
import com.example.ui.dialogs.CustomSymbolsManagerDialog
import com.example.ui.dialogs.ExportDialog
import com.example.ui.dialogs.ImportDialog
import com.example.ui.dialogs.NoteDefinitionsManagerDialog
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

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            viewModel.flushAutoSave()
        }
    }

    // Intercept back button to return to projects list
    BackHandler {
        viewModel.flushAutoSave()
        onBackToProjects()
    }

    var showPageManager by remember { mutableStateOf(false) }
    var showPageSettings by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showCustomSymbolsDialog by remember { mutableStateOf(false) }
    var showCategoryManagerDialog by remember { mutableStateOf(false) }
    var showNoteManagerDialog by remember { mutableStateOf(false) }

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
                hasSelection = state.selectedSymbolIds.isNotEmpty(),
                onBack = {
                    viewModel.flushAutoSave()
                    onBackToProjects()
                },
                onUndo = { viewModel.undo() },
                onRedo = { viewModel.redo() },
                onSave = { viewModel.saveProjectExplicitly() },
                onMoveUp = { viewModel.moveSelectedSymbolsVertical(directionUp = true) },
                onMoveDown = { viewModel.moveSelectedSymbolsVertical(directionUp = false) },
                onNoSelection = { viewModel.showNotification("ابتدا نماد مورد نظر را انتخاب کنید") },
                onOpenPageManager = { showPageManager = true },
                onOpenPageSettings = { showPageSettings = true },
                onOpenExport = { showExportDialog = true },
                onOpenImport = { showImportDialog = true },
                onOpenCustomSymbols = { showCustomSymbolsDialog = true }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            SymbolLibrarySheet(
                categories = state.categoriesList,
                activeCategoryId = state.activePaletteCategoryId,
                activeSymbol = state.activePaletteSymbol,
                activeCustomSymbolId = state.activeCustomSymbol?.id,
                customSymbols = state.customSymbolsList,
                noteDefinitions = state.noteDefinitionsList,
                onSelectCategory = { viewModel.selectCategory(it) },
                onSelectSymbol = { viewModel.selectPaletteSymbol(it) },
                onSelectCustomSymbol = { viewModel.selectCustomSymbol(it) },
                onOpenNoteManager = { showNoteManagerDialog = true },
                onOpenCategoryManager = { showCategoryManagerDialog = true },
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
                activePaletteSymbol = state.activePaletteSymbol,
                activeCustomSymbol = state.activeCustomSymbol,
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
                onRotate = { viewModel.rotateSelected() },
                onMoveUp = { viewModel.moveSelectedSymbolsVertical(directionUp = true) },
                onMoveDown = { viewModel.moveSelectedSymbolsVertical(directionUp = false) },
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
            onExportProjectFile = { viewModel.exportProjectFile(context) },
            onSavePdfToUri = { uri -> viewModel.savePdfToUri(context, uri) },
            onSaveImageToUri = { uri, isPng -> viewModel.saveImageToUri(context, uri, isPng) },
            onSaveProjectToUri = { uri -> viewModel.saveProjectToUri(context, uri) }
        )
    }

    if (showImportDialog) {
        ImportDialog(
            onDismiss = { showImportDialog = false },
            onImportContent = { content -> viewModel.importProjectContent(content) }
        )
    }

    if (showCustomSymbolsDialog) {
        CustomSymbolsManagerDialog(
            categories = state.categoriesList,
            customSymbols = state.customSymbolsList,
            onDismiss = { showCustomSymbolsDialog = false },
            onCreateFromUri = { uri, name, catId, isRing ->
                viewModel.createCustomSymbolFromUri(context, uri, name, catId, isRing)
            },
            onCreateCustom = { name, catId, isRing ->
                viewModel.createCustomSymbol(name, catId, isRing, null)
            },
            onUpdateCustom = { updated ->
                viewModel.updateCustomSymbol(updated)
            },
            onDeleteCustom = { id ->
                viewModel.removeCustomSymbol(id, context)
            },
            onSelectForPlacement = { custom ->
                viewModel.selectCustomSymbol(custom)
            },
            onReplaceImage = { sym, newUri ->
                viewModel.replaceCustomSymbolImage(context, sym, newUri)
            },
            onToggleHiddenCustomSymbol = { id, hidden ->
                viewModel.toggleCustomSymbolHidden(id, hidden)
            },
            onBulkImport = { items, renameDuplicates ->
                viewModel.importBulkSymbols(context, items, renameDuplicates)
            }
        )

    }

    if (showCategoryManagerDialog) {
        CategoryManagerDialog(
            categories = state.categoriesList,
            customSymbols = state.customSymbolsList,
            noteDefinitions = state.noteDefinitionsList,
            onDismiss = { showCategoryManagerDialog = false },
            onAddCategory = { viewModel.addCategory(it) },
            onUpdateCategory = { viewModel.updateCategory(it) },
            onDeleteCategorySafely = { viewModel.deleteCategorySafely(it) },
            onDeleteCategoryWithSymbols = { viewModel.deleteCategoryWithSymbols(it) },
            onDeleteCategoryAndMoveSymbols = { src, dst -> viewModel.deleteCategoryAndMoveSymbols(src, dst) }
        )
    }

    if (showNoteManagerDialog) {
        NoteDefinitionsManagerDialog(
            noteDefinitions = state.noteDefinitionsList,
            categories = state.categoriesList,
            onDismiss = { showNoteManagerDialog = false },
            onToggleHidden = { id, hidden ->
                viewModel.toggleNoteDefinitionHidden(id, hidden)
            },
            onAddNoteDefinition = { name, type, catId, dur, isRing ->
                viewModel.addNoteDefinition(name, type, catId, dur, isRing)
            },
            onUpdateNoteDefinition = { def ->
                viewModel.updateNoteDefinition(def)
            },
            onDeleteNoteDefinition = { id ->
                viewModel.deleteNoteDefinition(id)
            }
        )
    }
}
