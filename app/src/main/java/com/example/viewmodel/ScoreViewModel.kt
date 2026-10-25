package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.CategoryEntity
import com.example.data.CustomSymbolEntity
import com.example.data.FolderEntity
import com.example.data.HelpTopicEntity
import com.example.data.NoteDefinitionEntity
import com.example.data.ScoreRepository
import com.example.editor.UndoRedoManager
import com.example.io.ImportResult
import com.example.io.ScoreExporter
import com.example.io.ScoreImporter
import com.example.model.PaperSize
import com.example.model.ScorePage
import com.example.model.ScoreProject
import com.example.model.ScoreSymbol
import com.example.model.StaffSnapper
import com.example.model.SymbolCategory
import com.example.model.SymbolType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed class EditorScreen {
    object ProjectsList : EditorScreen()
    object ScoreEditor : EditorScreen()
    object PageManager : EditorScreen()
    object PageSettings : EditorScreen()
    object ExportDialog : EditorScreen()
    object ImportDialog : EditorScreen()
    object CustomSymbolsManager : EditorScreen()
}

data class EditorUiState(
    val currentScreen: EditorScreen = EditorScreen.ProjectsList,
    val currentProject: ScoreProject? = null,
    val projectsList: List<ScoreProject> = emptyList(),
    val foldersList: List<FolderEntity> = emptyList(),
    val categoriesList: List<CategoryEntity> = emptyList(),
    val noteDefinitionsList: List<NoteDefinitionEntity> = emptyList(),
    val customSymbolsList: List<CustomSymbolEntity> = emptyList(),
    val selectedSymbolIds: Set<String> = emptySet(),
    val activePaletteSymbol: SymbolType? = SymbolType.NOTE_QUARTER,
    val activeCustomSymbol: CustomSymbolEntity? = null,
    val activePaletteCategoryId: String = "NOTES",
    val activePaletteCategory: SymbolCategory = SymbolCategory.NOTES,
    val isSnapMode: Boolean = true,
    val isMultiSelectMode: Boolean = false,
    val showGuides: Boolean = true,
    val highlightLine4: Boolean = true,
    val zoomScale: Float = 1.0f,
    val panOffsetX: Float = 0f,
    val panOffsetY: Float = 0f,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val notificationMessage: String? = null,
    val referenceImageUri: String? = null,
    val showReferenceBackdrop: Boolean = false,
    val isSaving: Boolean = false,
    val helpTopicsList: List<HelpTopicEntity> = emptyList()
)

class ScoreViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ScoreRepository
    private val undoManager = UndoRedoManager()
    private val autoSaveChannel = Channel<ScoreProject>(Channel.CONFLATED)

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    init {
        val db = AppDatabase.getInstance(application)
        repository = ScoreRepository(db.projectDao(), application)

        viewModelScope.launch {
            repository.ensureDefaultCategories()
            repository.ensureDefaultNoteDefinitions()
            repository.ensureDefaultHelpTopics()
        }

        viewModelScope.launch(Dispatchers.IO) {
            autoSaveChannel.receiveAsFlow()
                .debounce(500L)
                .collectLatest { projectToSave ->
                    repository.saveProject(projectToSave)
                }
        }

        val prefs = application.getSharedPreferences("score_app_prefs", Context.MODE_PRIVATE)
        val hasSeeded = prefs.getBoolean("has_seeded_starter", false)

        viewModelScope.launch {
            repository.allProjects.collectLatest { list ->
                if (list.isEmpty() && !hasSeeded) {
                    prefs.edit().putBoolean("has_seeded_starter", true).apply()
                    val sample = repository.createDefaultStarterProject()
                    repository.saveProject(sample)
                } else {
                    _uiState.update { it.copy(projectsList = list) }
                }
            }
        }

        viewModelScope.launch {
            repository.allFolders.collectLatest { folders ->
                _uiState.update { it.copy(foldersList = folders) }
            }
        }

        viewModelScope.launch {
            repository.allCategories.collectLatest { cats ->
                _uiState.update { it.copy(categoriesList = cats) }
            }
        }

        viewModelScope.launch {
            repository.allNoteDefinitions.collectLatest { defs ->
                _uiState.update { it.copy(noteDefinitionsList = defs) }
            }
        }

        viewModelScope.launch {
            repository.allCustomSymbols.collectLatest { custom ->
                _uiState.update { it.copy(customSymbolsList = custom) }
            }
        }

        viewModelScope.launch {
            repository.allHelpTopics.collectLatest { topics ->
                _uiState.update { it.copy(helpTopicsList = topics) }
            }
        }
    }

    // --- NAVIGATION ---

    fun navigateTo(screen: EditorScreen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    fun openProject(project: ScoreProject) {
        undoManager.clear()
        val customMap = _uiState.value.customSymbolsList.associateBy { it.id }
        var hasResynced = false
        val syncedPages = project.pages.map { page ->
            val syncedSymbols = page.symbols.map { sym ->
                if (sym.type == SymbolType.CUSTOM_SYMBOL && sym.customData != null) {
                    val matchingCustom = customMap[sym.customData]
                    if (matchingCustom != null && !matchingCustom.imagePath.isNullOrBlank()) {
                        val currentFile = sym.imagePath?.let { java.io.File(it) }
                        if (currentFile == null || !currentFile.exists() || sym.imagePath != matchingCustom.imagePath) {
                            hasResynced = true
                            sym.copy(imagePath = matchingCustom.imagePath, name = matchingCustom.name, isRingSound = matchingCustom.isRingSound)
                        } else {
                            sym
                        }
                    } else {
                        sym
                    }
                } else {
                    sym
                }
            }
            page.copy(symbols = syncedSymbols)
        }
        val targetProject = if (hasResynced) project.copy(pages = syncedPages) else project

        _uiState.update {
            it.copy(
                currentProject = targetProject,
                currentScreen = EditorScreen.ScoreEditor,
                selectedSymbolIds = emptySet(),
                canUndo = false,
                canRedo = false,
                zoomScale = 1.0f,
                panOffsetX = 0f,
                panOffsetY = 0f
            )
        }
        if (hasResynced) {
            viewModelScope.launch { repository.saveProject(targetProject) }
        }
    }

    fun closeCurrentProject() {
        flushAutoSave()
        _uiState.update {
            it.copy(
                currentProject = null,
                currentScreen = EditorScreen.ProjectsList,
                selectedSymbolIds = emptySet()
            )
        }
    }

    fun createNewProject(title: String = "قطعه جدید") {
        val newProj = ScoreProject(
            title = title,
            pages = listOf(ScorePage(title = title))
        )
        viewModelScope.launch {
            repository.saveProject(newProj)
            openProject(newProj)
        }
    }

    fun duplicateProject(project: ScoreProject) {
        val copy = project.copy(
            id = java.util.UUID.randomUUID().toString(),
            title = "${project.title} (کپی)",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.saveProject(copy)
            showNotification("پروژه با موفقیت کپی شد")
        }
    }

    fun deleteProject(project: ScoreProject) {
        viewModelScope.launch {
            repository.deleteProject(project)
            if (_uiState.value.currentProject?.id == project.id) {
                _uiState.update { it.copy(currentProject = null, currentScreen = EditorScreen.ProjectsList) }
            }
            showNotification("پروژه حذف شد")
        }
    }

    // --- SELECTION & MODES ---

    fun selectPaletteSymbol(type: SymbolType) {
        _uiState.update { current ->
            if (current.activePaletteSymbol == type) {
                // Tapping active symbol again cancels the selection
                current.copy(activePaletteSymbol = null, activeCustomSymbol = null)
            } else {
                current.copy(
                    activePaletteSymbol = type,
                    activeCustomSymbol = null,
                    activePaletteCategory = type.defaultCategory,
                    activePaletteCategoryId = type.defaultCategory.name
                )
            }
        }
    }

    fun selectCustomSymbol(custom: CustomSymbolEntity) {
        _uiState.update { current ->
            if (current.activeCustomSymbol?.id == custom.id) {
                // Tapping active custom symbol again cancels selection
                current.copy(activeCustomSymbol = null, activePaletteSymbol = null)
            } else {
                current.copy(
                    activeCustomSymbol = custom,
                    activePaletteSymbol = null,
                    activePaletteCategoryId = custom.categoryId
                )
            }
        }
    }

    fun cancelPaletteSelection() {
        _uiState.update { it.copy(activePaletteSymbol = null, activeCustomSymbol = null) }
    }

    fun selectCategory(categoryId: String) {
        val catEnum = try {
            SymbolCategory.valueOf(categoryId)
        } catch (e: Exception) {
            SymbolCategory.CUSTOM
        }
        _uiState.update {
            it.copy(
                activePaletteCategoryId = categoryId,
                activePaletteCategory = catEnum
            )
        }
    }

    fun selectCategory(category: SymbolCategory) {
        selectCategory(category.name)
    }

    fun toggleSnapMode() {
        _uiState.update { it.copy(isSnapMode = !it.isSnapMode) }
    }

    fun toggleMultiSelectMode() {
        _uiState.update { it.copy(isMultiSelectMode = !it.isMultiSelectMode) }
    }

    fun toggleGuides() {
        _uiState.update { it.copy(showGuides = !it.showGuides) }
    }

    fun toggleLine4Highlight() {
        _uiState.update { it.copy(highlightLine4 = !it.highlightLine4) }
    }

    fun setZoomAndPan(zoom: Float, panX: Float, panY: Float) {
        _uiState.update {
            it.copy(
                zoomScale = zoom.coerceIn(com.example.editor.ScoreTransformMath.MIN_ZOOM, com.example.editor.ScoreTransformMath.MAX_ZOOM),
                panOffsetX = panX,
                panOffsetY = panY
            )
        }
    }

    fun resetZoom() {
        _uiState.update { it.copy(zoomScale = 1.0f, panOffsetX = 0f, panOffsetY = 0f) }
    }

    // --- CANVAS TAP & SYMBOL INTERACTION ---

    /**
     * Called when the user taps on the score sheet.
     * Can select an existing symbol or place the currently active palette symbol.
     */
    fun onCanvasTap(logicalX: Float, logicalY: Float) {
        val project = _uiState.value.currentProject ?: return
        val page = project.activePage
        val baseSpacing = page.lineSpacing

        // Check if user tapped an existing symbol (taking rotation into account)
        val hitSymbol = page.symbols.lastOrNull { sym ->
            val (hw, hh) = sym.getVisualBounds(baseSpacing)
            if (sym.rotation != 0f) {
                val dx = (logicalX - sym.x).toDouble()
                val dy = (logicalY - sym.y).toDouble()
                val rad = Math.toRadians(-sym.rotation.toDouble())
                val cos = Math.cos(rad)
                val sin = Math.sin(rad)
                val localX = (dx * cos - dy * sin).toFloat()
                val localY = (dx * sin + dy * cos).toFloat()
                localX in (-hw - 10f)..(hw + 10f) && localY in (-hh - 10f)..(hh + 10f)
            } else {
                val left = sym.x - hw - 8f
                val right = sym.x + hw + 8f
                val top = sym.y - hh - 8f
                val bottom = sym.y + hh + 8f
                logicalX in left..right && logicalY in top..bottom
            }
        }


        if (hitSymbol != null) {
            if (_uiState.value.isMultiSelectMode) {
                // Toggle selection
                val current = _uiState.value.selectedSymbolIds.toMutableSet()
                if (current.contains(hitSymbol.id)) {
                    current.remove(hitSymbol.id)
                } else {
                    current.add(hitSymbol.id)
                }
                _uiState.update { it.copy(selectedSymbolIds = current) }
            } else {
                _uiState.update { it.copy(selectedSymbolIds = setOf(hitSymbol.id)) }
            }
            return
        }

        // Tapped empty space
        if (_uiState.value.selectedSymbolIds.isNotEmpty() &&
            _uiState.value.activePaletteSymbol == null &&
            _uiState.value.activeCustomSymbol == null
        ) {
            // Deselect
            _uiState.update { it.copy(selectedSymbolIds = emptySet()) }
            return
        }

        // Place new custom symbol if active
        val customSymbol = _uiState.value.activeCustomSymbol
        if (customSymbol != null) {
            placeCustomSymbolAt(logicalX, logicalY, customSymbol)
            return
        }

        // Place new symbol from active palette
        val paletteSymbol = _uiState.value.activePaletteSymbol ?: return
        placeSymbolAt(logicalX, logicalY, paletteSymbol)
    }

    fun placeCustomSymbolAt(rawX: Float, rawY: Float, custom: CustomSymbolEntity) {
        val project = _uiState.value.currentProject ?: return
        val page = project.activePage
        val staves = page.generateStaves()
        val isRingSound = custom.isRingSound

        // Strict line 4 constraint for ring sounds!
        val (finalX, finalY) = StaffSnapper.calculatePosition(
            candidateX = rawX,
            candidateY = rawY,
            isRingSound = isRingSound,
            isSnapMode = _uiState.value.isSnapMode,
            staves = staves
        )

        recordUndo(page.symbols)

        val newSymbol = ScoreSymbol(
            type = SymbolType.CUSTOM_SYMBOL,
            category = SymbolCategory.CUSTOM,
            name = custom.name,
            x = finalX,
            y = finalY,
            isRingSound = isRingSound,
            imagePath = custom.imagePath,
            customData = custom.id
        )

        val updatedSymbols = page.symbols + newSymbol
        updateActivePageSymbols(updatedSymbols)
        _uiState.update { it.copy(selectedSymbolIds = setOf(newSymbol.id)) }
    }

    fun placeSymbolAt(rawX: Float, rawY: Float, type: SymbolType) {
        val project = _uiState.value.currentProject ?: return
        val page = project.activePage
        val staves = page.generateStaves()
        val isRingSound = type.isRingSound || type.defaultCategory == SymbolCategory.RING_SOUNDS

        // Strict line 4 constraint for ring sounds!
        val (finalX, finalY) = StaffSnapper.calculatePosition(
            candidateX = rawX,
            candidateY = rawY,
            isRingSound = isRingSound,
            isSnapMode = _uiState.value.isSnapMode,
            staves = staves
        )

        recordUndo(page.symbols)

        val newSymbol = ScoreSymbol(
            type = type,
            category = type.defaultCategory,
            name = type.defaultTitleFa,
            x = finalX,
            y = finalY,
            isRingSound = isRingSound
        )

        val updatedSymbols = page.symbols + newSymbol
        updateActivePageSymbols(updatedSymbols)
        _uiState.update { it.copy(selectedSymbolIds = setOf(newSymbol.id)) }
    }

    // --- SYMBOL MANIPULATION (DRAG, RESIZE, FLIP STEM, DELETE, DUPLICATE) ---

    fun onSymbolDrag(deltaX: Float, deltaY: Float) {
        val project = _uiState.value.currentProject ?: return
        val selectedIds = _uiState.value.selectedSymbolIds
        if (selectedIds.isEmpty()) return

        val page = project.activePage
        val staves = page.generateStaves()

        val updatedSymbols = page.symbols.map { sym ->
            if (selectedIds.contains(sym.id)) {
                val candidateX = sym.x + deltaX
                val candidateY = sym.y + deltaY

                if (sym.type == SymbolType.TEXT_ANNOTATION) {
                    sym.copy(x = candidateX, y = candidateY)
                } else {
                    val (fx, fy) = StaffSnapper.calculatePosition(
                        candidateX = candidateX,
                        candidateY = candidateY,
                        isRingSound = sym.isRingSound,
                        isSnapMode = _uiState.value.isSnapMode,
                        staves = staves
                    )
                    sym.copy(x = fx, y = fy)
                }
            } else {
                sym
            }
        }

        updateActivePageSymbols(updatedSymbols, recordHistory = false)
    }

    fun onSymbolDragFinished() {
        val project = _uiState.value.currentProject ?: return
        recordUndo(project.activePage.symbols)
        triggerAutoSave()
    }

    fun resizeSelected(scaleMultiplier: Float) {
        val project = _uiState.value.currentProject ?: return
        val selectedIds = _uiState.value.selectedSymbolIds
        if (selectedIds.isEmpty()) return

        val page = project.activePage
        recordUndo(page.symbols)

        val updated = page.symbols.map { sym ->
            if (selectedIds.contains(sym.id)) {
                val newScale = (sym.scale * scaleMultiplier).coerceIn(0.5f, 2.5f)
                sym.copy(scale = newScale)
            } else sym
        }
        updateActivePageSymbols(updated)
    }

    fun flipStemSelected() {
        val project = _uiState.value.currentProject ?: return
        val selectedIds = _uiState.value.selectedSymbolIds
        if (selectedIds.isEmpty()) return

        val page = project.activePage
        recordUndo(page.symbols)

        val updated = page.symbols.map { sym ->
            if (selectedIds.contains(sym.id)) {
                sym.copy(isStemUp = !sym.isStemUp)
            } else sym
        }
        updateActivePageSymbols(updated)
    }

    /**
     * Moves selected symbols vertically in increments of half staff line spacing (occupying lines & spaces).
     * Y decreases when moving UP and increases when moving DOWN.
     * Preserves relative positions across multi-selection and enforces line 4 for ring sounds.
     */
    fun moveSelectedSymbolsVertical(directionUp: Boolean) {
        val project = _uiState.value.currentProject ?: return
        val selectedIds = _uiState.value.selectedSymbolIds
        if (selectedIds.isEmpty()) {
            showNotification("ابتدا نماد مورد نظر را انتخاب کنید")
            return
        }

        val page = project.activePage
        val staves = page.generateStaves()
        // Configurable step based on staff line spacing
        val step = page.lineSpacing * 0.5f
        val deltaY = if (directionUp) -step else step

        recordUndo(page.symbols)

        val updatedSymbols = page.symbols.map { sym ->
            if (selectedIds.contains(sym.id)) {
                if (sym.isRingSound) {
                    val (fx, fy) = StaffSnapper.calculatePosition(
                        candidateX = sym.x,
                        candidateY = sym.y + deltaY,
                        isRingSound = true,
                        isSnapMode = false,
                        staves = staves
                    )
                    sym.copy(y = fy)
                } else {
                    val candidateY = sym.y + deltaY
                    val nearestStaff = staves.minByOrNull {
                        val centerY = (it.line1Y + it.line4Y) / 2f
                        Math.abs(centerY - candidateY)
                    }
                    val clampedY = if (nearestStaff != null) {
                        candidateY.coerceIn(nearestStaff.line4Y - page.lineSpacing * 3f, nearestStaff.line1Y + page.lineSpacing * 3f)
                    } else candidateY
                    sym.copy(y = clampedY)
                }
            } else {
                sym
            }
        }

        updateActivePageSymbols(updatedSymbols)
        triggerAutoSave()
    }

    /**
     * Rotates selected symbols by exactly 30 degrees clockwise.
     * 12 steps complete a full 360-degree rotation (0, 30, 60, ..., 330, 0).
     */
    fun rotateSelected() {
        val project = _uiState.value.currentProject ?: return
        val selectedIds = _uiState.value.selectedSymbolIds
        if (selectedIds.isEmpty()) {
            showNotification("ابتدا نماد مورد نظر را انتخاب کنید")
            return
        }

        val page = project.activePage
        recordUndo(page.symbols)

        val updatedSymbols = page.symbols.map { sym ->
            if (selectedIds.contains(sym.id)) {
                val nextRot = ((sym.rotation + 30f) % 360f).let { if (it < 0f) it + 360f else it }
                sym.copy(rotation = nextRot)
            } else {
                sym
            }
        }

        updateActivePageSymbols(updatedSymbols)
        triggerAutoSave()
    }


    fun duplicateSelected() {
        val project = _uiState.value.currentProject ?: return
        val selectedIds = _uiState.value.selectedSymbolIds
        if (selectedIds.isEmpty()) return

        val page = project.activePage
        recordUndo(page.symbols)

        val newClones = mutableListOf<ScoreSymbol>()
        val newSelectedIds = mutableSetOf<String>()

        for (sym in page.symbols) {
            if (selectedIds.contains(sym.id)) {
                val clone = sym.copy(
                    id = java.util.UUID.randomUUID().toString(),
                    x = sym.x + 25f,
                    // Ring sound Y stays strictly clamped to line 4!
                    y = if (sym.isRingSound) sym.y else sym.y + 10f
                )
                newClones.add(clone)
                newSelectedIds.add(clone.id)
            }
        }

        updateActivePageSymbols(page.symbols + newClones)
        _uiState.update { it.copy(selectedSymbolIds = newSelectedIds) }
        showNotification("نمادها تکثیر شدند")
    }

    fun deleteSelected() {
        val project = _uiState.value.currentProject ?: return
        val selectedIds = _uiState.value.selectedSymbolIds
        if (selectedIds.isEmpty()) return

        val page = project.activePage
        recordUndo(page.symbols)

        val remaining = page.symbols.filterNot { selectedIds.contains(it.id) }
        updateActivePageSymbols(remaining)
        _uiState.update { it.copy(selectedSymbolIds = emptySet()) }
        showNotification("نمادهای انتخاب‌شده حذف شدند")
    }

    // --- UNDO & REDO ---

    private fun recordUndo(symbols: List<ScoreSymbol>) {
        undoManager.recordState(symbols)
        _uiState.update { it.copy(canUndo = undoManager.canUndo, canRedo = undoManager.canRedo) }
    }

    fun undo() {
        val project = _uiState.value.currentProject ?: return
        val previous = undoManager.undo(project.activePage.symbols) ?: return
        updateActivePageSymbols(previous, recordHistory = false)
        _uiState.update {
            it.copy(
                canUndo = undoManager.canUndo,
                canRedo = undoManager.canRedo,
                selectedSymbolIds = emptySet()
            )
        }
        showNotification("عملیات لغو شد")
    }

    fun redo() {
        val project = _uiState.value.currentProject ?: return
        val next = undoManager.redo(project.activePage.symbols) ?: return
        updateActivePageSymbols(next, recordHistory = false)
        _uiState.update {
            it.copy(
                canUndo = undoManager.canUndo,
                canRedo = undoManager.canRedo,
                selectedSymbolIds = emptySet()
            )
        }
        showNotification("عملیات مجدداً انجام شد")
    }

    // --- PAGE MANAGEMENT ---

    fun addNewPage() {
        val project = _uiState.value.currentProject ?: return
        val templatePage = project.activePage
        val newPage = templatePage.copy(
            id = java.util.UUID.randomUUID().toString(),
            pageNumber = project.pages.size + 1,
            symbols = emptyList()
        )
        val updatedProject = project.copy(
            pages = project.pages + newPage,
            activePageIndex = project.pages.size
        )
        updateProject(updatedProject)
        undoManager.clear()
        _uiState.update { it.copy(canUndo = false, canRedo = false, selectedSymbolIds = emptySet()) }
        showNotification("صفحه جدید افزوده شد")
    }

    fun switchActivePage(index: Int) {
        val project = _uiState.value.currentProject ?: return
        if (index in project.pages.indices) {
            undoManager.clear()
            _uiState.update {
                it.copy(
                    currentProject = project.copy(activePageIndex = index),
                    selectedSymbolIds = emptySet(),
                    canUndo = false,
                    canRedo = false
                )
            }
        }
    }

    fun deleteCurrentPage() {
        val project = _uiState.value.currentProject ?: return
        if (project.pages.size <= 1) {
            showNotification("پروژه باید حداقل دارای یک صفحه باشد")
            return
        }

        val updatedPages = project.pages.toMutableList()
        val removeIndex = project.activePageIndex
        updatedPages.removeAt(removeIndex)

        // Renumber pages
        val renumbered = updatedPages.mapIndexed { idx, p -> p.copy(pageNumber = idx + 1) }
        val newIndex = (removeIndex - 1).coerceAtLeast(0)

        val updated = project.copy(pages = renumbered, activePageIndex = newIndex)
        updateProject(updated)
        undoManager.clear()
        _uiState.update { it.copy(canUndo = false, canRedo = false, selectedSymbolIds = emptySet()) }
        showNotification("صفحه حذف شد")
    }

    fun reorderPages(fromIndex: Int, toIndex: Int) {
        val project = _uiState.value.currentProject ?: return
        if (fromIndex !in project.pages.indices || toIndex !in project.pages.indices) return

        val pages = project.pages.toMutableList()
        val item = pages.removeAt(fromIndex)
        pages.add(toIndex, item)
        val renumbered = pages.mapIndexed { idx, p -> p.copy(pageNumber = idx + 1) }

        updateProject(project.copy(pages = renumbered, activePageIndex = toIndex))
    }

    fun updatePageSettings(
        paperSize: PaperSize,
        stavesCount: Int,
        lineSpacing: Float,
        line4SpacingRatio: Float,
        title: String,
        subtitle: String,
        lessonNumber: String
    ) {
        val project = _uiState.value.currentProject ?: return
        val updatedProject = project.updateActivePage { current ->
            current.copy(
                paperSize = paperSize,
                stavesCount = stavesCount.coerceIn(1, 16),
                lineSpacing = lineSpacing.coerceIn(10f, 26f),
                line4SpacingRatio = line4SpacingRatio.coerceIn(0.8f, 2.0f),
                title = title,
                subtitle = subtitle,
                lessonNumber = lessonNumber
            )
        }
        updateProject(updatedProject)
        showNotification("تنظیمات صفحه اعمال شد")
    }

    // --- PERSISTENCE & AUTO-SAVE ---

    private fun updateActivePageSymbols(symbols: List<ScoreSymbol>, recordHistory: Boolean = true) {
        val project = _uiState.value.currentProject ?: return
        if (recordHistory) {
            recordUndo(project.activePage.symbols)
        }
        val updatedProject = project.updateActivePage { it.copy(symbols = symbols) }
        _uiState.update { it.copy(currentProject = updatedProject) }
        triggerAutoSave()
    }

    private fun updateProject(project: ScoreProject) {
        _uiState.update { it.copy(currentProject = project) }
        triggerAutoSave()
    }

    fun saveProjectExplicitly() {
        val project = _uiState.value.currentProject ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isSaving = true) }
            repository.saveProject(project)
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(isSaving = false) }
                showNotification("پروژه ذخیره شد")
            }
        }
    }

    private fun triggerAutoSave() {
        val project = _uiState.value.currentProject ?: return
        autoSaveChannel.trySend(project)
    }

    fun flushAutoSave() {
        val project = _uiState.value.currentProject ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveProject(project)
        }
    }

    // --- CUSTOM SYMBOLS ---

    // --- CUSTOM SYMBOLS ---

    fun createCustomSymbolFromUri(
        context: Context,
        imageUri: Uri,
        name: String,
        categoryId: String,
        isRingSound: Boolean,
        onResult: ((Boolean, String?) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val (entity, error) = repository.addCustomSymbolFromUri(context, imageUri, name, categoryId, isRingSound)
            if (entity != null) {
                selectCustomSymbol(entity)
                val msg = "نماد سفارشی '$name' با موفقیت به کتابخانه افزوده شد"
                showNotification(msg)
                onResult?.invoke(true, null)
            } else {
                val errorMsg = error ?: "خطا در پردازش یا ذخیره‌سازی نماد سفارشی"
                showNotification(errorMsg)
                onResult?.invoke(false, errorMsg)
            }
        }
    }

    fun replaceCustomSymbolImage(
        context: Context,
        symbol: CustomSymbolEntity,
        newImageUri: Uri,
        onResult: ((Boolean, String?) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val oldPath = symbol.imagePath
            val (updated, error) = repository.replaceCustomSymbolImage(context, symbol, newImageUri)
            if (updated != null) {
                if (oldPath != null) {
                    com.example.util.CustomSymbolImageHelper.evict(oldPath)
                }
                if (_uiState.value.activeCustomSymbol?.id == updated.id) {
                    _uiState.update { it.copy(activeCustomSymbol = updated) }
                }
                val proj = _uiState.value.currentProject
                if (proj != null) {
                    var modified = false
                    val updatedPages = proj.pages.map { page ->
                        val newSymbols = page.symbols.map { s ->
                            if (s.customData == updated.id || (oldPath != null && s.imagePath == oldPath)) {
                                modified = true
                                s.copy(imagePath = updated.imagePath, name = updated.name, isRingSound = updated.isRingSound)
                            } else {
                                s
                            }
                        }
                        page.copy(symbols = newSymbols)
                    }
                    if (modified) {
                        val updatedProj = proj.copy(pages = updatedPages, updatedAt = System.currentTimeMillis())
                        _uiState.update { it.copy(currentProject = updatedProj) }
                        repository.saveProject(updatedProj)
                    }
                }
                showNotification("تصویر نماد سفارشی '${updated.name}' با موفقیت تغییر یافت")
                onResult?.invoke(true, null)
            } else {
                val errorMsg = error ?: "خطا در تعویض تصویر نماد"
                showNotification(errorMsg)
                onResult?.invoke(false, errorMsg)
            }
        }
    }

    fun createCustomSymbol(
        name: String,
        categoryId: String = "CUSTOM",
        isRingSound: Boolean = false,
        vectorPath: String? = null
    ) {
        viewModelScope.launch {
            val entity = repository.addCustomSymbol(name, categoryId, isRingSound, vectorPath)
            selectCustomSymbol(entity)
            showNotification("نماد سفارشی '$name' با موفقیت افزوده شد")
        }
    }

    fun updateCustomSymbol(symbol: CustomSymbolEntity) {
        viewModelScope.launch {
            repository.updateCustomSymbol(symbol)
            if (_uiState.value.activeCustomSymbol?.id == symbol.id) {
                _uiState.update { it.copy(activeCustomSymbol = symbol) }
            }
            val proj = _uiState.value.currentProject
            if (proj != null) {
                var modified = false
                val updatedPages = proj.pages.map { page ->
                    val newSymbols = page.symbols.map { s ->
                        if (s.customData == symbol.id) {
                            modified = true
                            s.copy(name = symbol.name, isRingSound = symbol.isRingSound, imagePath = symbol.imagePath ?: s.imagePath)
                        } else {
                            s
                        }
                    }
                    page.copy(symbols = newSymbols)
                }
                if (modified) {
                    val updatedProj = proj.copy(pages = updatedPages, updatedAt = System.currentTimeMillis())
                    _uiState.update { it.copy(currentProject = updatedProj) }
                    repository.saveProject(updatedProj)
                }
            }
            showNotification("اطلاعات نماد سفارشی '${symbol.name}' بروزرسانی شد")
        }
    }

    fun removeCustomSymbol(id: String, context: Context? = null) {
        viewModelScope.launch {
            repository.deleteCustomSymbol(id, context)
            if (_uiState.value.activeCustomSymbol?.id == id) {
                _uiState.update { it.copy(activeCustomSymbol = null) }
            }
            showNotification("نماد سفارشی حذف شد")
        }
    }

    // --- CATEGORIES & NOTE DEFINITIONS ---

    fun addCategory(titleFa: String) {
        viewModelScope.launch {
            repository.addCategory(titleFa)
            showNotification("دسته‌بندی '$titleFa' افزوده شد")
        }
    }

    fun updateCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.updateCategory(category)
            showNotification("دسته‌بندی بروزرسانی شد")
        }
    }

    fun deleteCategorySafely(categoryId: String) {
        viewModelScope.launch {
            repository.deleteCategorySafely(categoryId)
            if (_uiState.value.activePaletteCategoryId == categoryId) {
                selectCategory("NOTES")
            }
            showNotification("دسته‌بندی با حفظ نمادها حذف شد")
        }
    }

    fun deleteCategoryWithSymbols(categoryId: String) {
        viewModelScope.launch {
            repository.deleteCategoryWithSymbols(categoryId)
            if (_uiState.value.activePaletteCategoryId == categoryId) {
                selectCategory("NOTES")
            }
            showNotification("دسته‌بندی و تمام نمادهای آن حذف شدند")
        }
    }

    fun deleteCategoryAndMoveSymbols(categoryId: String, targetCategoryId: String) {
        viewModelScope.launch {
            repository.deleteCategoryAndMoveSymbols(categoryId, targetCategoryId)
            if (_uiState.value.activePaletteCategoryId == categoryId) {
                selectCategory(targetCategoryId)
            }
            showNotification("دسته‌بندی حذف و نمادها منتقل شدند")
        }
    }

    fun importBulkSymbols(
        context: Context,
        itemsToImport: List<com.example.io.BulkImportItem>,
        renameDuplicates: Boolean,
        onDone: (Int) -> Unit = {}
    ) {
        viewModelScope.launch {
            val count = com.example.io.BulkSymbolImporter.executeImport(
                context = context,
                itemsToImport = itemsToImport,
                renameDuplicates = renameDuplicates
            ) { entity ->
                repository.insertCustomSymbol(entity)
            }
            showNotification("$count نماد با موفقیت درون‌ریزی شد")
            onDone(count)
        }
    }

    fun addNoteDefinition(
        name: String,
        symbolType: SymbolType,
        categoryId: String,
        duration: String,
        isRingSound: Boolean
    ) {
        viewModelScope.launch {
            repository.addNoteDefinition(
                name = name,
                symbolType = symbolType,
                categoryId = categoryId,
                duration = duration,
                isRingSound = isRingSound,
                imagePath = null
            )
            showNotification("تعریف نت '$name' افزوده شد")
        }
    }

    fun updateNoteDefinition(def: NoteDefinitionEntity) {
        viewModelScope.launch {
            repository.updateNoteDefinition(def)
            showNotification("تعریف نت '${def.name}' بروزرسانی شد")
        }
    }

    fun deleteNoteDefinition(id: String) {
        viewModelScope.launch {
            val def = _uiState.value.noteDefinitionsList.find { it.id == id }
            repository.deleteNoteDefinition(id)
            if (def != null && _uiState.value.activePaletteSymbol?.name == def.symbolTypeName) {
                _uiState.update { it.copy(activePaletteSymbol = null) }
            }
            showNotification("تعریف نت حذف شد")
        }
    }

    fun toggleNoteDefinitionHidden(id: String, hidden: Boolean) {
        viewModelScope.launch {
            repository.hideNoteDefinition(id, hidden)
        }
    }

    fun toggleCustomSymbolHidden(id: String, hidden: Boolean) {
        viewModelScope.launch {
            repository.hideCustomSymbol(id, hidden)
        }
    }

    // --- TEXT ANNOTATIONS ---

    fun addTextAnnotation(
        text: String,
        fontSizeSp: Float = 14f,
        isBold: Boolean = false,
        isItalic: Boolean = false,
        alignment: String = "right",
        colorHex: String = "#1C1A18",
        candidateX: Float? = null,
        candidateY: Float? = null
    ) {
        val project = _uiState.value.currentProject ?: return
        val page = project.activePage

        val posX = candidateX ?: (page.paperSize.widthPt / 2f)
        val posY = candidateY ?: (page.marginTop - 20f)

        recordUndo(page.symbols)

        val stylingData = "bold=$isBold;italic=$isItalic;align=$alignment;fontSize=$fontSizeSp"
        val scaleVal = (fontSizeSp / 14f).coerceIn(0.5f, 3.5f)

        val newSymbol = ScoreSymbol(
            type = SymbolType.TEXT_ANNOTATION,
            category = SymbolCategory.TEMPO_TEXT,
            name = text.take(15),
            x = posX,
            y = posY,
            scale = scaleVal,
            textValue = text,
            customData = stylingData,
            colorHex = colorHex
        )

        val updated = page.symbols + newSymbol
        updateActivePageSymbols(updated)
        _uiState.update { it.copy(selectedSymbolIds = setOf(newSymbol.id)) }
        showNotification("یادداشت متنی افزوده شد")
    }

    fun updateTextAnnotation(
        symbolId: String,
        text: String,
        fontSizeSp: Float = 14f,
        isBold: Boolean = false,
        isItalic: Boolean = false,
        alignment: String = "right",
        colorHex: String = "#1C1A18"
    ) {
        val project = _uiState.value.currentProject ?: return
        val page = project.activePage

        recordUndo(page.symbols)

        val stylingData = "bold=$isBold;italic=$isItalic;align=$alignment;fontSize=$fontSizeSp"
        val scaleVal = (fontSizeSp / 14f).coerceIn(0.5f, 3.5f)

        val updated = page.symbols.map { sym ->
            if (sym.id == symbolId) {
                sym.copy(
                    textValue = text,
                    name = text.take(15),
                    customData = stylingData,
                    scale = scaleVal,
                    colorHex = colorHex
                )
            } else sym
        }

        updateActivePageSymbols(updated)
        showNotification("یادداشت متنی بروزرسانی شد")
    }

    // --- HELP TOPICS ---

    fun addHelpTopic(title: String, content: String) {
        viewModelScope.launch {
            repository.addHelpTopic(title, content)
            showNotification("مبحث راهنما افزوده شد")
        }
    }

    fun updateHelpTopic(topic: HelpTopicEntity) {
        viewModelScope.launch {
            repository.updateHelpTopic(topic)
            showNotification("مبحث راهنما بروزرسانی شد")
        }
    }

    fun deleteHelpTopic(id: String) {
        viewModelScope.launch {
            repository.deleteHelpTopic(id)
            showNotification("مبحث راهنما حذف شد")
        }
    }

    fun reorderHelpTopics(reordered: List<HelpTopicEntity>) {
        viewModelScope.launch {
            repository.reorderHelpTopics(reordered)
        }
    }

    fun resetHelpTopicsToDefault() {
        viewModelScope.launch {
            repository.resetHelpTopicsToDefault()
            showNotification("مباحث راهنما به حالت پیش‌فرض بازنشانی شدند")
        }
    }

    // --- IMPORT & EXPORT ACTIONS ---

    fun exportToPdf(context: Context) {
        val project = _uiState.value.currentProject ?: return
        try {
            val file = ScoreExporter.exportToPdf(context, project)
            ScoreExporter.shareFile(context, file, "application/pdf", "اشتراک‌گذاری فایل PDF نت‌نویسی")
            showNotification("فایل PDF با موفقیت آماده شد")
        } catch (e: Exception) {
            showNotification("خطا در صدور PDF: ${e.localizedMessage}")
        }
    }

    fun exportActivePageImage(context: Context, isPng: Boolean = true) {
        val project = _uiState.value.currentProject ?: return
        try {
            val file = ScoreExporter.exportPageToImage(context, project.activePage, isPng = isPng)
            val mime = if (isPng) "image/png" else "image/jpeg"
            ScoreExporter.shareFile(context, file, mime, "اشتراک‌گذاری تصویر صفحه")
            showNotification("تصویر صفحه با کیفیت بالا ایجاد شد")
        } catch (e: Exception) {
            showNotification("خطا در ایجاد تصویر: ${e.localizedMessage}")
        }
    }

    fun exportProjectFile(context: Context) {
        val project = _uiState.value.currentProject ?: return
        try {
            val file = ScoreExporter.exportProjectFile(context, project)
            ScoreExporter.shareFile(context, file, "application/json", "ارسال فایل قابل ویرایش پروژه")
            showNotification("فایل پروژه با موفقیت صادر شد")
        } catch (e: Exception) {
            showNotification("خطا در صدور پروژه: ${e.localizedMessage}")
        }
    }

    fun savePdfToUri(context: Context, uri: Uri) {
        val project = _uiState.value.currentProject ?: return
        try {
            val ok = ScoreExporter.writePdfToUri(context, uri, project)
            if (ok) {
                showNotification("فایل PDF با موفقیت در محل انتخابی ذخیره شد")
            } else {
                showNotification("خطا در ذخیره‌سازی فایل PDF در مسیر انتخابی")
            }
        } catch (e: Exception) {
            showNotification("خطا در نوشتن فایل PDF: ${e.localizedMessage}")
        }
    }

    fun saveImageToUri(context: Context, uri: Uri, isPng: Boolean = true) {
        val project = _uiState.value.currentProject ?: return
        try {
            val ok = ScoreExporter.writeImageToUri(context, uri, project.activePage, isPng = isPng)
            if (ok) {
                showNotification("تصویر صفحه با موفقیت در محل انتخابی ذخیره شد")
            } else {
                showNotification("خطا در ذخیره‌سازی تصویر در مسیر انتخابی")
            }
        } catch (e: Exception) {
            showNotification("خطا در ذخیره تصویر: ${e.localizedMessage}")
        }
    }

    fun saveProjectToUri(context: Context, uri: Uri) {
        val project = _uiState.value.currentProject ?: return
        try {
            val ok = ScoreExporter.writeProjectToUri(context, uri, project)
            if (ok) {
                showNotification("فایل پروژه (.dafnt) با موفقیت در محل انتخابی ذخیره شد")
            } else {
                showNotification("خطا در ذخیره فایل پروژه")
            }
        } catch (e: Exception) {
            showNotification("خطا در ذخیره پروژه: ${e.localizedMessage}")
        }
    }

    fun importProjectContent(jsonOrXml: String) {
        if (jsonOrXml.contains("score-partwise") || jsonOrXml.contains("<?xml")) {
            val res = ScoreImporter.parseMusicXml(jsonOrXml)
            if (res.isSuccess && res.data != null) {
                viewModelScope.launch {
                    repository.saveProject(res.data)
                    openProject(res.data)
                }
                val incomp = res.report?.incompatibleElements?.joinToString("، ") ?: ""
                val msg = "نت‌های MusicXML وارد شدند. (${res.report?.importedNotesCount} نت)" +
                        if (incomp.isNotBlank()) "\nعناصر تطبیق‌یافته: $incomp" else ""
                showNotification(msg)
            } else {
                showNotification(res.errorMessage ?: "خطا در خواندن MusicXML")
            }
        } else {
            val res = ScoreImporter.parseProjectJson(jsonOrXml)
            if (res.isSuccess && res.data != null) {
                viewModelScope.launch {
                    repository.saveProject(res.data)
                    openProject(res.data)
                }
                showNotification("پروژه با موفقیت بارگذاری شد")
            } else {
                showNotification(res.errorMessage ?: "فرمت فایل نامعتبر است")
            }
        }
    }

    fun setReferenceImageUri(uri: String?) {
        _uiState.update {
            it.copy(
                referenceImageUri = uri,
                showReferenceBackdrop = uri != null
            )
        }
    }

    fun toggleReferenceBackdrop() {
        _uiState.update { it.copy(showReferenceBackdrop = !it.showReferenceBackdrop) }
    }

    fun showNotification(msg: String) {
        _uiState.update { it.copy(notificationMessage = msg) }
    }

    fun clearNotification() {
        _uiState.update { it.copy(notificationMessage = null) }
    }
}
