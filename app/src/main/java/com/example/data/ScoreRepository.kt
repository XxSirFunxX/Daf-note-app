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
        list.map { entity ->
            val parsedResult = ScoreImporter.parseProjectJson(entity.jsonContent)
            if (parsedResult.isSuccess && parsedResult.data != null) {
                parsedResult.data
            } else {
                ScoreProject(
                    id = entity.id,
                    title = entity.title,
                    author = entity.author,
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt,
                    folderName = entity.folderName,
                    isCorrupted = true,
                    rawCorruptJson = entity.jsonContent,
                    parseError = parsedResult.errorMessage ?: "خطای ساختار فایل پروژه"
                )
            }
        }
    }

    val allFolders: Flow<List<FolderEntity>> = dao.getAllFolders()
    val allCategories: Flow<List<CategoryEntity>> = dao.getAllCategories()
    val allNoteDefinitions: Flow<List<NoteDefinitionEntity>> = dao.getAllNoteDefinitions()
    val activeNoteDefinitions: Flow<List<NoteDefinitionEntity>> = dao.getAllActiveNoteDefinitions()
    val allCustomSymbols: Flow<List<CustomSymbolEntity>> = dao.getAllCustomSymbols()
    val activeCustomSymbols: Flow<List<CustomSymbolEntity>> = dao.getAllActiveCustomSymbols()
    val allHelpTopics: Flow<List<HelpTopicEntity>> = dao.getAllHelpTopics()

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

    private fun markCategoryDeleted(categoryId: String) {
        val prefs = context?.getSharedPreferences("score_app_prefs", Context.MODE_PRIVATE) ?: return
        val currentSet = prefs.getStringSet("deleted_default_cats", emptySet())?.toMutableSet() ?: mutableSetOf()
        currentSet.add(categoryId)
        prefs.edit().putStringSet("deleted_default_cats", currentSet).apply()
    }

    private fun markNoteDefDeleted(noteId: String) {
        val prefs = context?.getSharedPreferences("score_app_prefs", Context.MODE_PRIVATE) ?: return
        val currentSet = prefs.getStringSet("deleted_default_notes", emptySet())?.toMutableSet() ?: mutableSetOf()
        currentSet.add(noteId)
        prefs.edit().putStringSet("deleted_default_notes", currentSet).apply()
    }

    suspend fun ensureDefaultCategories() {
        val prefs = context?.getSharedPreferences("score_app_prefs", Context.MODE_PRIVATE)
        val hasSeeded = prefs?.getBoolean("has_seeded_default_cats", false) ?: false
        if (hasSeeded) return

        val deletedCats = prefs?.getStringSet("deleted_default_cats", emptySet()) ?: emptySet()
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
            ).filter { !deletedCats.contains(it.id) }
            if (defaults.isNotEmpty()) {
                dao.insertCategories(defaults)
            }
            prefs?.edit()?.putBoolean("has_seeded_default_cats", true)?.apply()
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
        markCategoryDeleted(categoryId)
        dao.reassignCategoryNotes(categoryId, fallbackCategoryId)
        dao.reassignCategoryCustomSymbols(categoryId, "CUSTOM")
        dao.deleteCategory(categoryId)
    }

    suspend fun deleteCategoryWithSymbols(categoryId: String) {
        markCategoryDeleted(categoryId)
        val notes = dao.getAllNoteDefinitions().firstOrNull()?.filter { it.categoryId == categoryId } ?: emptyList()
        notes.forEach { 
            markNoteDefDeleted(it.id)
            deleteNoteDefinitionSafely(it.id) 
        }
        val customs = dao.getAllCustomSymbols().firstOrNull()?.filter { it.categoryId == categoryId } ?: emptyList()
        customs.forEach { deleteCustomSymbol(it.id) }
        dao.deleteCategory(categoryId)
    }

    suspend fun deleteCategoryAndMoveSymbols(categoryId: String, targetCategoryId: String) {
        markCategoryDeleted(categoryId)
        dao.reassignCategoryNotes(categoryId, targetCategoryId)
        dao.reassignCategoryCustomSymbols(categoryId, targetCategoryId)
        dao.deleteCategory(categoryId)
    }

    // --- NOTE DEFINITIONS MANAGEMENT ---

    suspend fun ensureDefaultNoteDefinitions() {
        val prefs = context?.getSharedPreferences("score_app_prefs", Context.MODE_PRIVATE)
        val hasSeeded = prefs?.getBoolean("has_seeded_default_notes", false) ?: false
        if (hasSeeded) return

        val deletedNotes = prefs?.getStringSet("deleted_default_notes", emptySet()) ?: emptySet()
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
            ).filter { !deletedNotes.contains(it.id) }
            if (defaults.isNotEmpty()) {
                dao.insertNoteDefinitions(defaults)
            }
            prefs?.edit()?.putBoolean("has_seeded_default_notes", true)?.apply()
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

    suspend fun addNoteDefinitionFromUri(
        context: Context,
        imageUri: Uri?,
        name: String,
        symbolType: SymbolType,
        categoryId: String,
        duration: String,
        isRingSound: Boolean
    ): Pair<NoteDefinitionEntity?, String?> {
        val id = UUID.randomUUID().toString()
        var savedImagePath: String? = null

        if (imageUri != null) {
            val bytes = try {
                context.contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
            } catch (e: Exception) {
                return Pair(null, "عدم امکان دسترسی به تصویر انتخاب شده: ${e.localizedMessage}")
            }

            if (bytes != null && bytes.isNotEmpty()) {
                val declaredMime = context.contentResolver.getType(imageUri)
                val validation = com.example.util.CustomSymbolImageHelper.validateImage(bytes, declaredMime)
                if (validation is com.example.util.CustomSymbolImageHelper.ValidationResult.Error) {
                    return Pair(null, validation.messageFa)
                }

                val isSvg = (validation as com.example.util.CustomSymbolImageHelper.ValidationResult.Success).isSvg
                val ext = if (isSvg) "svg" else "png"
                val symbolsDir = File(context.filesDir, "symbols").apply { mkdirs() }
                val destFile = File(symbolsDir, "notedef_${id}.$ext")
                try {
                    FileOutputStream(destFile).use { it.write(bytes) }
                    savedImagePath = destFile.absolutePath
                } catch (e: Exception) {
                    return Pair(null, "خطا در ذخیره‌سازی تصویر در حافظه دستگاه: ${e.localizedMessage}")
                }
            }
        }

        val def = NoteDefinitionEntity(
            id = id,
            name = name,
            symbolTypeName = symbolType.name,
            categoryId = categoryId,
            durationValue = duration,
            isRingSound = isRingSound,
            isDefault = false,
            isHidden = false,
            imagePath = savedImagePath
        )
        dao.insertNoteDefinition(def)
        return Pair(def, null)
    }

    suspend fun updateNoteDefinitionWithImage(
        context: Context,
        def: NoteDefinitionEntity,
        newImageUri: Uri?,
        removeImage: Boolean = false
    ): Pair<NoteDefinitionEntity?, String?> {
        var newPath = def.imagePath

        if (removeImage) {
            def.imagePath?.let { p ->
                try { File(p).delete() } catch (ignored: Exception) {}
            }
            newPath = null
        } else if (newImageUri != null) {
            val bytes = try {
                context.contentResolver.openInputStream(newImageUri)?.use { it.readBytes() }
            } catch (e: Exception) {
                return Pair(null, "عدم امکان دسترسی به تصویر جدید: ${e.localizedMessage}")
            }

            if (bytes == null || bytes.isEmpty()) {
                return Pair(null, "تصویر انتخابی خالی است.")
            }

            val declaredMime = context.contentResolver.getType(newImageUri)
            val validation = com.example.util.CustomSymbolImageHelper.validateImage(bytes, declaredMime)
            if (validation is com.example.util.CustomSymbolImageHelper.ValidationResult.Error) {
                return Pair(null, validation.messageFa)
            }

            val isSvg = (validation as com.example.util.CustomSymbolImageHelper.ValidationResult.Success).isSvg
            val ext = if (isSvg) "svg" else "png"
            val symbolsDir = File(context.filesDir, "symbols").apply { mkdirs() }
            val newFile = File(symbolsDir, "notedef_${def.id}_${System.currentTimeMillis()}.$ext")

            try {
                FileOutputStream(newFile).use { it.write(bytes) }
                def.imagePath?.let { p ->
                    try { File(p).delete() } catch (ignored: Exception) {}
                }
                newPath = newFile.absolutePath
            } catch (e: Exception) {
                return Pair(null, "خطا در ذخیره تصویر جدید: ${e.localizedMessage}")
            }
        }

        val updated = def.copy(imagePath = newPath)
        dao.updateNoteDefinition(updated)
        return Pair(updated, null)
    }

    suspend fun updateNoteDefinition(def: NoteDefinitionEntity) {
        dao.updateNoteDefinition(def)
    }

    suspend fun deleteNoteDefinitionSafely(id: String) {
        val all = dao.getAllNoteDefinitions().firstOrNull() ?: emptyList()
        val target = all.find { it.id == id }
        target?.imagePath?.let { path ->
            val otherNoteDefRefs = all.any { it.id != id && it.imagePath == path }
            val customSymbolRefs = dao.getAllCustomSymbols().firstOrNull()?.any { it.imagePath == path } ?: false
            val projectRefs = dao.getAllProjects().firstOrNull()?.any { p ->
                val content = p.jsonContent.replace("\\/", "/")
                content.contains(path)
            } ?: false
            if (!otherNoteDefRefs && !customSymbolRefs && !projectRefs) {
                try { File(path).delete() } catch (ignored: Exception) {}
            }
        }
        markNoteDefDeleted(id)
        dao.deleteNoteDefinition(id)
    }

    suspend fun deleteNoteDefinition(id: String) {
        deleteNoteDefinitionSafely(id)
    }

    suspend fun hideNoteDefinition(id: String, hidden: Boolean) {
        dao.setNoteDefinitionHidden(id, hidden)
    }

    suspend fun hideCustomSymbol(id: String, hidden: Boolean) {
        dao.setCustomSymbolHidden(id, hidden)
    }

    // --- CUSTOM SYMBOLS MANAGEMENT WITH PERSISTENT INTERNAL STORAGE ---

    /**
     * Imports a user-selected PNG or SVG image, validates it, saves a permanent local copy
     * in the app's internal filesDir, and creates a CustomSymbolEntity.
     */
    suspend fun addCustomSymbolFromUri(
        context: Context,
        imageUri: Uri,
        name: String,
        categoryId: String = "CUSTOM",
        isRingSound: Boolean = false
    ): Pair<CustomSymbolEntity?, String?> {
        val id = UUID.randomUUID().toString()
        val symbolsDir = File(context.filesDir, "symbols").apply { mkdirs() }

        val bytes = try {
            context.contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
        } catch (e: Exception) {
            return Pair(null, "عدم امکان دسترسی به فایل انتخاب شده: ${e.localizedMessage}")
        }

        if (bytes == null || bytes.isEmpty()) {
            return Pair(null, "فایل انتخاب شده خالی یا غیرقابل خواندن است.")
        }

        val declaredMime = context.contentResolver.getType(imageUri)
        val validation = com.example.util.CustomSymbolImageHelper.validateImage(bytes, declaredMime)
        if (validation is com.example.util.CustomSymbolImageHelper.ValidationResult.Error) {
            return Pair(null, validation.messageFa)
        }

        val isSvg = (validation as com.example.util.CustomSymbolImageHelper.ValidationResult.Success).isSvg
        val ext = if (isSvg) "svg" else "png"
        val destFile = File(symbolsDir, "sym_$id.$ext")

        return try {
            FileOutputStream(destFile).use { it.write(bytes) }
            val entity = CustomSymbolEntity(
                id = id,
                name = name,
                categoryId = categoryId,
                isRingSound = isRingSound,
                imagePath = destFile.absolutePath,
                createdAt = System.currentTimeMillis()
            )
            dao.insertCustomSymbol(entity)
            Pair(entity, null)
        } catch (e: Exception) {
            Pair(null, "خطا در ذخیره‌سازی نماد در حافظه دستگاه: ${e.localizedMessage}")
        }
    }

    suspend fun replaceCustomSymbolImage(
        context: Context,
        symbol: CustomSymbolEntity,
        newImageUri: Uri
    ): Pair<CustomSymbolEntity?, String?> {
        val bytes = try {
            context.contentResolver.openInputStream(newImageUri)?.use { it.readBytes() }
        } catch (e: Exception) {
            return Pair(null, "عدم امکان خواندن تصویر جدید: ${e.localizedMessage}")
        }

        if (bytes == null || bytes.isEmpty()) {
            return Pair(null, "تصویر انتخابی خالی است.")
        }

        val declaredMime = context.contentResolver.getType(newImageUri)
        val validation = com.example.util.CustomSymbolImageHelper.validateImage(bytes, declaredMime)
        if (validation is com.example.util.CustomSymbolImageHelper.ValidationResult.Error) {
            return Pair(null, validation.messageFa)
        }

        val isSvg = (validation as com.example.util.CustomSymbolImageHelper.ValidationResult.Success).isSvg
        val ext = if (isSvg) "svg" else "png"
        val symbolsDir = File(context.filesDir, "symbols").apply { mkdirs() }
        val newFile = File(symbolsDir, "sym_${symbol.id}_${System.currentTimeMillis()}.$ext")

        return try {
            FileOutputStream(newFile).use { it.write(bytes) }
            // Delete old file if existed
            symbol.imagePath?.let { oldPath ->
                try { File(oldPath).delete() } catch (ignored: Exception) {}
            }
            val updated = symbol.copy(imagePath = newFile.absolutePath)
            dao.updateCustomSymbol(updated)
            Pair(updated, null)
        } catch (e: Exception) {
            Pair(null, "خطا در جایگزینی فایل تصویر: ${e.localizedMessage}")
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

    suspend fun insertCustomSymbol(symbol: CustomSymbolEntity) {
        dao.insertCustomSymbol(symbol)
    }

    suspend fun updateCustomSymbol(symbol: CustomSymbolEntity) {
        dao.updateCustomSymbol(symbol)
    }

    suspend fun deleteCustomSymbol(id: String, context: Context? = null) {
        val all = dao.getAllCustomSymbols().firstOrNull() ?: emptyList()
        val target = all.find { it.id == id }
        target?.imagePath?.let { path ->
            val otherRefs = all.any { it.id != id && it.imagePath == path }
            val noteDefRefs = dao.getAllNoteDefinitions().firstOrNull()?.any { it.imagePath == path } ?: false
            val projectRefs = dao.getAllProjects().firstOrNull()?.any { p ->
                val content = p.jsonContent.replace("\\/", "/")
                content.contains(path)
            } ?: false
            if (!otherRefs && !noteDefRefs && !projectRefs) {
                try { File(path).delete() } catch (ignored: Exception) {}
            }
        }
        dao.deleteCustomSymbol(id)
    }

    // --- HELP TOPICS MANAGEMENT ---

    suspend fun ensureDefaultHelpTopics() {
        val existing = dao.getAllHelpTopics().firstOrNull()
        if (existing.isNullOrEmpty()) {
            val defaults = getDefaultHelpTopics()
            dao.insertHelpTopics(defaults)
        }
    }

    fun getDefaultHelpTopics(): List<HelpTopicEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            HelpTopicEntity(
                id = "HELP_STAFF_BASICS",
                title = "معرفی حامل چهارخطی دف و تفاوت با ۵ خطی کلاسیک",
                content = "ساز دف به عنوان ساز کوبه‌ای، به جای حامل پنج‌خطی استاندارد غربی از خط حامل چهارخطی بهره می‌برد.\n\n" +
                        "• خط ۱ (پایین‌ترین خط): صدای بَم یا تُم (با دست راست یا چپ روی کاسه/پوست).\n" +
                        "• خط ۲ و ۳ (خطوط و فواصل میانی): صداهای میانه و تکنیک‌های چپ و راست، اشاره‌ها و غلتک‌ها.\n" +
                        "• خط ۴ (بالاترین خط): منحصراً برای صدای حلقه‌ها و زنجیرهای دف تخصیص یافته است.",
                orderIndex = 0,
                isDefault = true,
                updatedAt = now
            ),
            HelpTopicEntity(
                id = "HELP_RING_RULE",
                title = "قانون انحصاری خط چهارم (صدای حلقه‌ها)",
                content = "در نت‌نگار دف، تمام نمادهای متعلق به دسته «صدای حلقه‌ها» (شامل حلقه منفرد، ریز حلقه، اشاره حلقه، حلقه جفتی و شستی یا پرتاب حلقه) به صورت هوشمند و خودکار منحصراً روی خط چهارم حامل قرار می‌گیرند.\n\n" +
                        "در هنگام جابه‌جایی این نمادها، موقعیت عمودی آنها روی خط چهارم حفظ شده و تغییر خط نمی‌دهند تا استاندارد نت‌نویسی دف مخدوش نگردد.",
                orderIndex = 1,
                isDefault = true,
                updatedAt = now
            ),
            HelpTopicEntity(
                id = "HELP_ADDING_NOTES",
                title = "نت‌نویسی و جای‌گذاری نشانه‌ها",
                content = "برای افزودن نت به صفحه:\n" +
                        "۱. از پالت پایین صفحه، دسته‌بندی مورد نظر (نت‌ها، سکوت‌ها، حلقه‌ها، تکنیک‌ها و ...) را انتخاب کنید.\n" +
                        "۲. روی نشانه یا نت مورد نظر بزنید تا فعال شود.\n" +
                        "۳. روی هر نقطه‌ای از خط حامل بزنید تا نشانه در آن نقطه قرار گیرد.\n" +
                        "۴. در صورت فعال بودن حالت «چسبیدن به خطوط» (آیکون شبکه در نوار سریع)، نشانه‌ها دقیقا روی خطوط و میان‌خطوط حامل تنظیم می‌شوند.",
                orderIndex = 2,
                isDefault = true,
                updatedAt = now
            ),
            HelpTopicEntity(
                id = "HELP_SELECTION_DRAG",
                title = "انتخاب و جابه‌جایی نمادها",
                content = "• انتخاب تک‌نماد: برای انتخاب هر نماد، کافیست روی آن لمس کنید. کادر نارنجی دور نماد ظاهر می‌شود.\n" +
                        "• حالت چندانتخابی: با فعال کردن آیکون چندانتخابی در نوار سریع، می‌توانید چندین نماد را همزمان انتخاب کرده و با هم جابه‌جا یا ویرایش نمایید.\n" +
                        "• کشیدن و رها کردن (Drag): نماد انتخاب‌شده را با انگشت روی صفحه به چپ، راست، بالا یا پایین بکشید.",
                orderIndex = 3,
                isDefault = true,
                updatedAt = now
            ),
            HelpTopicEntity(
                id = "HELP_QUICK_TOOLBAR",
                title = "نوار ابزار سریع و کلیدهای میانبر",
                content = "وقتی نمادی را انتخاب می‌کنید، نوار شناور گزینه‌های زیر را در اختیارتان می‌گذارد:\n" +
                        "• فلش‌های بالا و پایین: انتقال عمودی نت بین خطوط و فواصل حامل بدون کشیدن دستی.\n" +
                        "• چرخش ۳۰ درجه: چرخش زاویه‌ای نماد با هر بار لمس.\n" +
                        "• بزرگ‌نمایی (+) و کوچک‌نمایی (-): تغییر مقیاس اندازه نماد.\n" +
                        "• معکوس‌کردن دم نت: تغییر جهت دم نت به بالا یا پایین.\n" +
                        "• کپی (Duplicate): تکثیر سریع نمادهای انتخاب‌شده.\n" +
                        "• حذف (Delete): پاک کردن نماد از روی خط حامل.",
                orderIndex = 4,
                isDefault = true,
                updatedAt = now
            ),
            HelpTopicEntity(
                id = "HELP_CUSTOM_IMAGES",
                title = "افزودن و مدیریت نمادهای تصویری (PNG و SVG)",
                content = "شما می‌توانید هر نماد، نشانه ابداعی یا رسم‌الخط اختصاصی را با فرمت PNG یا SVG وارد کنید:\n" +
                        "• از منوی بالای ویرایشگر یا دکمه «جدید» در پالت سفارشی، گزینه مدیریت نمادهای سفارشی را انتخاب نمایید.\n" +
                        "• تصاویر با پس‌زمینه شفاف (Transparent) بهترین کیفیت نمایش را دارند.\n" +
                        "• تمام تصاویر وارد شده در حافظه داخلی دستگاه و کش با سرعت بالا ذخیره می‌شوند و حتی پس از ذخیره و بازگشایی پروژه همواره حفظ خواهند بود.",
                orderIndex = 5,
                isDefault = true,
                updatedAt = now
            ),
            HelpTopicEntity(
                id = "HELP_TEXT_ANNOTATIONS",
                title = "یادداشت‌ها و متن‌های سفارشی روی حامل",
                content = "برای افزودن متن، اشعار، نام بندها (مانند «بند اول» یا «تکرار») و نشانه‌های تمپو و توضیحات معلم روی حامل:\n" +
                        "• روی دکمه ابزار متن (آیکون T) در نوار ابزار کلیک کنید.\n" +
                        "• متن دلخواه را بنویسید، اندازه قلم، حالت ضخیم (Bold)، کج (Italic)، تراز و رنگ قلم را مشخص کنید.\n" +
                        "• متن روی حامل قرار می‌گیرد و می‌توانید آن را آزادانه جابه‌جا، بچرخانید و دوباره ویرایش نمایید.",
                orderIndex = 6,
                isDefault = true,
                updatedAt = now
            ),
            HelpTopicEntity(
                id = "HELP_PAGE_SETTINGS",
                title = "مدیریت صفحات و تنظیمات خطوط حامل",
                content = "از منوی بالای ویرایشگر می‌توانید:\n" +
                        "• مدیریت صفحات: افزودن صفحات جدید، تغییر ترتیب صفحات و حذف صفحه فعال.\n" +
                        "• تنظیمات صفحه: تغییر قطع کاغذ (A4 یا A5)، تعیین تعداد میزان‌ها و خطوط حامل در هر صفحه، فاصله پیش‌فرض خطوط و فاصله اختصاصی خط چهارم حلقه‌ها.",
                orderIndex = 7,
                isDefault = true,
                updatedAt = now
            ),
            HelpTopicEntity(
                id = "HELP_EXPORT_BACKUP",
                title = "صدور خروجی PDF، تصویر و فایل پروژه (.dafnt)",
                content = "نت‌نگار دف امکان دریافت خروجی‌های حرفه‌ای را فراهم می‌کند:\n" +
                        "• خروجی PDF: سندی استاندارد و با کیفیت وکتور، آماده برای چاپ روی کاغذ و انتشار کتابچه آموزشی.\n" +
                        "• خروجی تصویر (PNG و JPEG): با وضوح بالا برای اشتراک‌گذاری در پیام‌رسان‌ها.\n" +
                        "• خروجی فایل پروژه (.dafnt): تمام نت‌ها، نشانه‌های سفارشی و متون در یک فایل اختصاصی ذخیره می‌شوند تا بتوانید در دستگاه دیگر یا در آینده مجدداً آن را ویرایش کنید.",
                orderIndex = 8,
                isDefault = true,
                updatedAt = now
            )
        )
    }

    suspend fun addHelpTopic(title: String, content: String): HelpTopicEntity {
        val all = dao.getAllHelpTopics().firstOrNull() ?: emptyList()
        val nextOrder = (all.maxOfOrNull { it.orderIndex } ?: -1) + 1
        val entity = HelpTopicEntity(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            content = content.trim(),
            orderIndex = nextOrder,
            isDefault = false,
            updatedAt = System.currentTimeMillis()
        )
        dao.insertHelpTopic(entity)
        return entity
    }

    suspend fun updateHelpTopic(topic: HelpTopicEntity) {
        dao.updateHelpTopic(topic.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteHelpTopic(id: String) {
        dao.deleteHelpTopic(id)
    }

    suspend fun reorderHelpTopics(reorderedTopics: List<HelpTopicEntity>) {
        val updated = reorderedTopics.mapIndexed { index, topic ->
            topic.copy(orderIndex = index, updatedAt = System.currentTimeMillis())
        }
        dao.insertHelpTopics(updated)
    }

    suspend fun resetHelpTopicsToDefault() {
        dao.deleteAllHelpTopics()
        val defaults = getDefaultHelpTopics()
        dao.insertHelpTopics(defaults)
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
            title = "درس نمونه دف",
            author = "استاد موسیقی",
            pages = listOf(page.copy(symbols = symbols))
        )
    }
}
