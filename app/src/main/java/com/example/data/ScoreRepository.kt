package com.example.data

import android.content.Context
import android.net.Uri
import com.example.io.ScoreExporter
import com.example.io.ScoreImporter
import com.example.model.PaperSize
import com.example.model.ScorePage
import com.example.model.ScoreProject
import com.example.model.ScoreSymbol
import com.example.model.SymbolCategory
import com.example.model.SymbolType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class ScoreRepository(
    private val dao: ProjectDao,
    private val context: Context? = null
) {

    val allProjects: Flow<List<ScoreProject>> = dao.getAllProjects().map { list ->
        list.mapNotNull { entity ->
            ScoreImporter.parseProjectJson(entity.jsonContent).data
        }
    }

    val allFolders: Flow<List<FolderEntity>> = dao.getAllFolders()
    val allCategories: Flow<List<CategoryEntity>> = dao.getAllCategories()
    val allNoteDefinitions: Flow<List<NoteDefinitionEntity>> = dao.getAllActiveNoteDefinitions()
    val allCustomSymbols: Flow<List<CustomSymbolEntity>> = dao.getAllCustomSymbols()

    suspend fun getProjectById(id: String): ScoreProject? {
        val entity = dao.getProjectById(id) ?: return null
        return ScoreImporter.parseProjectJson(entity.jsonContent).data
    }

    suspend fun saveProject(project: ScoreProject) {
        val json = ScoreExporter.projectToJson(project)
        val entity = ProjectEntity(
            id = project.id,
            title = project.title,
            author = project.author,
            createdAt = project.createdAt,
            updatedAt = System.currentTimeMillis(),
            folderName = project.folderName ?: "عمومی",
            jsonContent = json
        )
        dao.insertOrUpdateProject(entity)
    }

    suspend fun deleteProject(project: ScoreProject) {
        val json = ScoreExporter.projectToJson(project)
        dao.deleteProject(
            ProjectEntity(
                id = project.id,
                title = project.title,
                author = project.author,
                createdAt = project.createdAt,
                updatedAt = project.updatedAt,
                folderName = project.folderName ?: "عمومی",
                jsonContent = json
            )
        )
    }

    suspend fun addFolder(name: String) {
        dao.insertFolder(FolderEntity(id = UUID.randomUUID().toString(), name = name))
    }

    suspend fun deleteFolder(folder: FolderEntity) {
        dao.deleteFolder(folder)
    }

    // --- CATEGORY MANAGEMENT ---

    suspend fun ensureDefaultCategories() {
        val existing = dao.getAllCategories().firstOrNull()
        if (existing.isNullOrEmpty()) {
            val defaults = listOf(
                CategoryEntity("NOTES", "نت‌ها", 0, isDefault = true),
                CategoryEntity("RESTS", "سکوت‌ها", 1, isDefault = true),
                CategoryEntity("RING_SOUNDS", "صدای حلقه‌ها", 2, isDefault = true),
                CategoryEntity("TECHNIQUES", "تکنیک و مضراب", 3, isDefault = true),
                CategoryEntity("BAR_LINES", "خطوط میزان", 4, isDefault = true),
                CategoryEntity("TIME_SIGNATURES", "میزان‌نما", 5, isDefault = true),
                CategoryEntity("TEMPO_TEXT", "متن و علائم", 6, isDefault = true),
                CategoryEntity("CUSTOM", "سفارشی", 7, isDefault = true)
            )
            dao.insertCategories(defaults)
        }
    }

    suspend fun addCategory(titleFa: String): CategoryEntity {
        val id = UUID.randomUUID().toString()
        val cat = CategoryEntity(id = id, titleFa = titleFa, sortOrder = 10, isDefault = false)
        dao.insertCategory(cat)
        return cat
    }

    suspend fun updateCategory(category: CategoryEntity) {
        dao.updateCategory(category)
    }

    /**
     * Deletes a category safely: reassigns its note definitions and custom symbols
     * to the fallback category ("NOTES" or "CUSTOM") so no symbol is ever orphaned.
     */
    suspend fun deleteCategorySafely(categoryId: String, fallbackCategoryId: String = "NOTES") {
        dao.reassignCategoryNotes(categoryId, fallbackCategoryId)
        dao.reassignCategoryCustomSymbols(categoryId, "CUSTOM")
        dao.deleteCategory(categoryId)
    }

    // --- NOTE DEFINITIONS MANAGEMENT ---

    suspend fun ensureDefaultNoteDefinitions() {
        val existing = dao.getAllNoteDefinitions().firstOrNull()
        if (existing.isNullOrEmpty()) {
            val defaults = listOf(
                NoteDefinitionEntity("DEF_WHOLE", "نت گرد", "NOTE_WHOLE", "NOTES", durationValue = "1", isDefault = true),
                NoteDefinitionEntity("DEF_HALF", "نت سفید", "NOTE_HALF", "NOTES", durationValue = "1/2", isDefault = true),
                NoteDefinitionEntity("DEF_QUARTER", "نت سیاه", "NOTE_QUARTER", "NOTES", durationValue = "1/4", isDefault = true),
                NoteDefinitionEntity("DEF_EIGHTH", "نت چنگ", "NOTE_EIGHTH", "NOTES", durationValue = "1/8", isDefault = true),
                NoteDefinitionEntity("DEF_16TH", "نت دولاچنگ", "NOTE_SIXTEENTH", "NOTES", durationValue = "1/16", isDefault = true),
                NoteDefinitionEntity("DEF_32ND", "نت سه‌لاچنگ", "NOTE_THIRTYSECOND", "NOTES", durationValue = "1/32", isDefault = true),
                NoteDefinitionEntity("DEF_64TH", "نت چهارلاچنگ", "NOTE_SIXTYFOURTH", "NOTES", durationValue = "1/64", isDefault = true),

                NoteDefinitionEntity("DEF_R_WHOLE", "سکوت گرد", "REST_WHOLE", "RESTS", durationValue = "1", isDefault = true),
                NoteDefinitionEntity("DEF_R_HALF", "سکوت سفید", "REST_HALF", "RESTS", durationValue = "1/2", isDefault = true),
                NoteDefinitionEntity("DEF_R_QUARTER", "سکوت سیاه", "REST_QUARTER", "RESTS", durationValue = "1/4", isDefault = true),
                NoteDefinitionEntity("DEF_R_EIGHTH", "سکوت چنگ", "REST_EIGHTH", "RESTS", durationValue = "1/8", isDefault = true),

                NoteDefinitionEntity("DEF_RING_SINGLE", "حلقه منفرد", "RING_SINGLE", "RING_SOUNDS", isRingSound = true, isDefault = true),
                NoteDefinitionEntity("DEF_RING_TREMOLO", "ریز حلقه", "RING_TREMOLO", "RING_SOUNDS", isRingSound = true, isDefault = true),
                NoteDefinitionEntity("DEF_RING_ACCENT", "اشاره حلقه", "RING_ACCENT", "RING_SOUNDS", isRingSound = true, isDefault = true),
                NoteDefinitionEntity("DEF_RING_DOUBLE", "حلقه جفتی", "RING_DOUBLE", "RING_SOUNDS", isRingSound = true, isDefault = true),
                NoteDefinitionEntity("DEF_RING_THUMB", "شستی / پرتاب", "RING_THUMB_ROLL", "RING_SOUNDS", isRingSound = true, isDefault = true)
            )
            dao.insertNoteDefinitions(defaults)
        }
    }

    suspend fun addNoteDefinition(
        name: String,
        symbolType: SymbolType,
        categoryId: String,
        duration: String,
        isRingSound: Boolean,
        imagePath: String?
    ): NoteDefinitionEntity {
        val def = NoteDefinitionEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            symbolTypeName = symbolType.name,
            categoryId = categoryId,
            durationValue = duration,
            isRingSound = isRingSound,
            isDefault = false,
            isHidden = false,
            imagePath = imagePath
        )
        dao.insertNoteDefinition(def)
        return def
    }

    suspend fun updateNoteDefinition(def: NoteDefinitionEntity) {
        dao.updateNoteDefinition(def)
    }

    suspend fun deleteNoteDefinition(id: String) {
        dao.deleteNoteDefinition(id)
    }

    suspend fun hideNoteDefinition(id: String, hidden: Boolean) {
        dao.setNoteDefinitionHidden(id, hidden)
    }

    // --- CUSTOM SYMBOLS MANAGEMENT WITH PERSISTENT INTERNAL STORAGE ---

    /**
     * Imports a user-selected PNG or SVG image, saves a permanent local copy
     * in the app's internal filesDir, and creates a CustomSymbolEntity.
     */
    suspend fun addCustomSymbolFromUri(
        context: Context,
        imageUri: Uri,
        name: String,
        categoryId: String = "CUSTOM",
        isRingSound: Boolean = false
    ): CustomSymbolEntity? {
        val id = UUID.randomUUID().toString()
        val symbolsDir = File(context.filesDir, "symbols").apply { mkdirs() }
        val ext = if (context.contentResolver.getType(imageUri)?.contains("svg") == true) "svg" else "png"
        val destFile = File(symbolsDir, "sym_$id.$ext")

        return try {
            context.contentResolver.openInputStream(imageUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            val entity = CustomSymbolEntity(
                id = id,
                name = name,
                categoryId = categoryId,
                isRingSound = isRingSound,
                imagePath = destFile.absolutePath,
                createdAt = System.currentTimeMillis()
            )
            dao.insertCustomSymbol(entity)
            entity
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun addCustomSymbol(
        name: String,
        categoryId: String = "CUSTOM",
        isRingSound: Boolean = false,
        vectorData: String? = null
    ): CustomSymbolEntity {
        val entity = CustomSymbolEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            categoryId = categoryId,
            isRingSound = isRingSound,
            vectorData = vectorData
        )
        dao.insertCustomSymbol(entity)
        return entity
    }

    suspend fun updateCustomSymbol(symbol: CustomSymbolEntity) {
        dao.updateCustomSymbol(symbol)
    }

    suspend fun deleteCustomSymbol(id: String) {
        dao.deleteCustomSymbol(id)
    }

    /**
     * Creates an authentic starter sample lesson matching the user's reference images.
     */
    fun createDefaultStarterProject(): ScoreProject {
        val page = ScorePage(
            title = "درس ۱: دو ضربی ساده",
            subtitle = "این درس، دو ضربی ساده است و هر ضرب آن یک سیاه می‌باشد.",
            lessonNumber = "۱",
            paperSize = PaperSize.A4,
            stavesCount = 4
        )

        val staves = page.generateStaves()
        val symbols = mutableListOf<ScoreSymbol>()

        if (staves.isNotEmpty()) {
            val staff1 = staves[0]

            // 1. Time signature 2/4 at beginning
            symbols.add(
                ScoreSymbol(
                    type = SymbolType.TIME_2_4,
                    x = staff1.left + 35f,
                    y = (staff1.line1Y + staff1.line4Y) / 2f
                )
            )

            // 2. Bar 1: Quarter note (Tom - line 1) + Quarter rest
            symbols.add(
                ScoreSymbol(
                    type = SymbolType.NOTE_QUARTER,
                    x = staff1.left + 90f,
                    y = staff1.line1Y,
                    isStemUp = true
                )
            )
            symbols.add(
                ScoreSymbol(
                    type = SymbolType.TECH_TOM,
                    x = staff1.left + 90f,
                    y = staff1.line1Y + 22f,
                    textValue = "۱"
                )
            )
            symbols.add(
                ScoreSymbol(
                    type = SymbolType.REST_QUARTER,
                    x = staff1.left + 150f,
                    y = staff1.line2Y
                )
            )
            symbols.add(
                ScoreSymbol(
                    type = SymbolType.BAR_SINGLE,
                    x = staff1.left + 200f,
                    y = (staff1.line1Y + staff1.line4Y) / 2f
                )
            )

            // 3. Bar 2: Quarter note (Mianeh - line 2) + Quarter rest
            symbols.add(
                ScoreSymbol(
                    type = SymbolType.NOTE_QUARTER,
                    x = staff1.left + 250f,
                    y = staff1.line2Y,
                    isStemUp = true
                )
            )
            symbols.add(
                ScoreSymbol(
                    type = SymbolType.REST_QUARTER,
                    x = staff1.left + 310f,
                    y = staff1.line2Y
                )
            )
            symbols.add(
                ScoreSymbol(
                    type = SymbolType.BAR_SINGLE,
                    x = staff1.left + 360f,
                    y = (staff1.line1Y + staff1.line4Y) / 2f
                )
            )

            // 4. Bar 3: Quarter note (Line 3) + Quarter rest
            symbols.add(
                ScoreSymbol(
                    type = SymbolType.NOTE_QUARTER,
                    x = staff1.left + 410f,
                    y = staff1.line3Y,
                    isStemUp = false
                )
            )
            symbols.add(
                ScoreSymbol(
                    type = SymbolType.REST_QUARTER,
                    x = staff1.left + 470f,
                    y = staff1.line2Y
                )
            )
            symbols.add(
                ScoreSymbol(
                    type = SymbolType.BAR_SINGLE,
                    x = staff1.left + 520f,
                    y = (staff1.line1Y + staff1.line4Y) / 2f
                )
            )

            // 5. Bar 4: RING SOUND (صدای حلقه‌ها - Line 4 strictly!) + Quarter note
            symbols.add(
                ScoreSymbol(
                    type = SymbolType.RING_SINGLE,
                    x = staff1.left + 580f,
                    y = staff1.line4Y,
                    isRingSound = true
                )
            )
            symbols.add(
                ScoreSymbol(
                    type = SymbolType.NOTE_QUARTER,
                    x = staff1.left + 640f,
                    y = staff1.line1Y,
                    isStemUp = true
                )
            )
            symbols.add(
                ScoreSymbol(
                    type = SymbolType.BAR_FINAL,
                    x = staff1.left + 700f,
                    y = (staff1.line1Y + staff1.line4Y) / 2f
                )
            )

            // Technique markings (راست و چپ)
            symbols.add(
                ScoreSymbol(
                    type = SymbolType.TECH_RAST,
                    x = staff1.left + 90f,
                    y = staff1.line1Y - 20f
                )
            )
            symbols.add(
                ScoreSymbol(
                    type = SymbolType.TECH_CHAP,
                    x = staff1.left + 250f,
                    y = staff1.line2Y - 20f
                )
            )
        }

        return ScoreProject(
            title = "درس نمونه چهارخطی",
            author = "استاد موسیقی",
            pages = listOf(page.copy(symbols = symbols))
        )
    }
}
