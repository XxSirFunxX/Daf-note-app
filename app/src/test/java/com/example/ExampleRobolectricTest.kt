package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.io.ScoreExporter
import com.example.io.ScoreImporter
import com.example.model.PaperSize
import com.example.model.ScorePage
import com.example.model.ScoreProject
import com.example.model.ScoreSymbol
import com.example.model.SymbolType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.CategoryEntity
import com.example.data.CustomSymbolEntity
import com.example.data.ScoreRepository
import com.example.editor.ScoreTransformMath
import com.example.io.BulkSymbolImporter
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun readAppNameFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("نت نگار دف", appName)
    }

    @Test
    fun testProjectSerializationAndDeserialization() {
        val project = ScoreProject(
            title = "پروژه آزمایشی",
            author = "نویسنده",
            pages = listOf(
                ScorePage(
                    title = "درس ۱",
                    symbols = listOf(
                        ScoreSymbol(type = SymbolType.RING_SINGLE, x = 120f, y = 80f, isRingSound = true),
                        ScoreSymbol(type = SymbolType.NOTE_QUARTER, x = 180f, y = 140f, isStemUp = true)
                    )
                )
            )
        )

        val json = ScoreExporter.projectToJson(project)
        val importResult = ScoreImporter.parseProjectJson(json)

        assertTrue(importResult.isSuccess)
        val parsed = importResult.data
        assertNotNull(parsed)
        assertEquals("پروژه آزمایشی", parsed?.title)
        assertEquals(1, parsed?.pages?.size)
        assertEquals(2, parsed?.pages?.first()?.symbols?.size)
        assertEquals(SymbolType.RING_SINGLE, parsed?.pages?.first()?.symbols?.get(0)?.type)
        assertTrue(parsed?.pages?.first()?.symbols?.get(0)?.isRingSound == true)
    }

    @Test
    fun exportToImage_generatesValidImageFile() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val page = ScorePage(title = "صفحه تصویر")

        val imageFile = ScoreExporter.exportPageToImage(context, page, isPng = true)
        assertNotNull(imageFile)
        assertTrue(imageFile.exists())
        assertTrue(imageFile.length() > 0)
    }

    @Test
    fun testCustomSymbolSerialization_preservesAllMetadata() {
        val customSym = ScoreSymbol(
            type = SymbolType.CUSTOM_SYMBOL,
            category = com.example.model.SymbolCategory.CUSTOM,
            name = "حلقه ترکیبی ویژه",
            x = 220f,
            y = 80f,
            isRingSound = true,
            imagePath = "/data/data/com.daf.notenegar/files/symbols/sym_1.png",
            customData = "custom_id_123"
        )

        val project = ScoreProject(
            title = "پروژه با نماد سفارشی",
            pages = listOf(ScorePage(symbols = listOf(customSym)))
        )

        val json = ScoreExporter.projectToJson(project)
        val res = ScoreImporter.parseProjectJson(json)

        assertTrue(res.isSuccess)
        val importedSym = res.data?.pages?.first()?.symbols?.first()
        assertNotNull(importedSym)
        assertEquals(SymbolType.CUSTOM_SYMBOL, importedSym?.type)
        assertEquals("حلقه ترکیبی ویژه", importedSym?.name)
        assertTrue(importedSym?.isRingSound == true)
        assertEquals("/data/data/com.daf.notenegar/files/symbols/sym_1.png", importedSym?.imagePath)
        assertEquals("custom_id_123", importedSym?.customData)
    }

    @Test
    fun testStaffSnapper_strictlyEnforcesLine4ForCustomRingSounds() {
        val page = ScorePage()
        val staves = page.generateStaves()
        assertTrue(staves.isNotEmpty())
        val staff = staves[0]

        // Candidate Y near line 1 (Bottom line)
        val candidateX = staff.left + 100f
        val candidateY = staff.line1Y + 2f

        // When isRingSound = true, it MUST snap to line 4 (top line)
        val (_, finalRingY) = com.example.model.StaffSnapper.calculatePosition(
            candidateX = candidateX,
            candidateY = candidateY,
            isRingSound = true,
            isSnapMode = true,
            staves = staves
        )
        assertEquals(staff.line4Y, finalRingY, 0.01f)

        // When isRingSound = false, it can snap to line 1
        val (_, finalNormalY) = com.example.model.StaffSnapper.calculatePosition(
            candidateX = candidateX,
            candidateY = candidateY,
            isRingSound = false,
            isSnapMode = true,
            staves = staves
        )
        assertEquals(staff.line1Y, finalNormalY, 0.01f)
    }

    @Test
    fun testSvgValidationAndRasterization() {
        val validSvg = """
            <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100" viewBox="0 0 100 100">
                <circle cx="50" cy="50" r="40" stroke="green" stroke-width="4" fill="yellow" />
            </svg>
        """.trimIndent().toByteArray(Charsets.UTF_8)

        val validation = com.example.util.CustomSymbolImageHelper.validateImage(validSvg, "image/svg+xml")
        assertTrue(validation is com.example.util.CustomSymbolImageHelper.ValidationResult.Success)
        assertTrue((validation as com.example.util.CustomSymbolImageHelper.ValidationResult.Success).isSvg)

        val bmp = com.example.util.CustomSymbolImageHelper.renderSvgToBitmap(validSvg.inputStream(), 80, 80)
        assertNotNull(bmp)
        assertEquals(80, bmp?.width)
        assertEquals(80, bmp?.height)
    }

    @Test
    fun testInvalidImageValidation_returnsMeaningfulErrorMessage() {
        val invalidBytes = "This is not an image or SVG file at all".toByteArray(Charsets.UTF_8)
        val validation = com.example.util.CustomSymbolImageHelper.validateImage(invalidBytes, "text/plain")
        assertTrue(validation is com.example.util.CustomSymbolImageHelper.ValidationResult.Error)
        val errorMsg = (validation as com.example.util.CustomSymbolImageHelper.ValidationResult.Error).messageFa
        assertTrue(errorMsg.contains("پشتیبانی نمی‌شود") || errorMsg.contains("نامعتبر"))
    }

    @Test
    fun testCustomSymbolDatabaseCRUD() = kotlinx.coroutines.test.runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = com.example.data.AppDatabase.getInstance(context)
        val dao = db.projectDao()

        val symbolId = "test_sym_${System.currentTimeMillis()}"
        val initialSymbol = com.example.data.CustomSymbolEntity(
            id = symbolId,
            name = "نماد مضراب چپ",
            categoryId = "CUSTOM",
            isRingSound = false,
            imagePath = "/data/data/symbols/sym1.png"
        )

        // 1. Insert
        dao.insertCustomSymbol(initialSymbol)
        val listAfterInsert = dao.getAllCustomSymbols().first()
        assertTrue(listAfterInsert.any { it.id == symbolId && it.name == "نماد مضراب چپ" })

        // 2. Update metadata & replace image
        val updatedSymbol = initialSymbol.copy(
            name = "نماد مضراب چپ (ویرایش)",
            isRingSound = true,
            imagePath = "/data/data/symbols/sym1_new.svg"
        )
        dao.updateCustomSymbol(updatedSymbol)
        val listAfterUpdate = dao.getAllCustomSymbols().first()
        val found = listAfterUpdate.find { it.id == symbolId }
        assertNotNull(found)
        assertEquals("نماد مضراب چپ (ویرایش)", found?.name)
        assertTrue(found?.isRingSound == true)
        assertEquals("/data/data/symbols/sym1_new.svg", found?.imagePath)

        // 3. Delete
        dao.deleteCustomSymbol(symbolId)
        val listAfterDelete = dao.getAllCustomSymbols().first()
        assertTrue(listAfterDelete.none { it.id == symbolId })
    }

    @Test
    fun testNoteDefinitionsCRUDAndHiding() = kotlinx.coroutines.test.runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = com.example.data.AppDatabase.getInstance(context)
        val dao = db.projectDao()

        val defId = "custom_def_test"
        val noteDef = com.example.data.NoteDefinitionEntity(
            id = defId,
            name = "نت دولاچنگ سرعتی",
            symbolTypeName = "NOTE_SIXTEENTH",
            categoryId = "NOTES",
            durationValue = "1/16",
            isRingSound = false,
            isDefault = false,
            isHidden = false
        )

        // Insert
        dao.insertNoteDefinition(noteDef)
        val allDefs = dao.getAllNoteDefinitions().first()
        assertTrue(allDefs.any { it.id == defId })

        // Hide
        dao.setNoteDefinitionHidden(defId, true)
        val activeDefs = dao.getAllActiveNoteDefinitions().first()
        assertTrue(activeDefs.none { it.id == defId })

        // Unhide
        dao.setNoteDefinitionHidden(defId, false)
        val activeDefs2 = dao.getAllActiveNoteDefinitions().first()
        assertTrue(activeDefs2.any { it.id == defId })

        // Clean up
        dao.deleteNoteDefinition(defId)
    }

    @Test
    fun testCategorySafeDeletion_reassignsSymbols() = kotlinx.coroutines.test.runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = com.example.data.AppDatabase.getInstance(context)
        val dao = db.projectDao()

        val catId = "CUSTOM_CAT_TEMP"
        dao.insertCategory(com.example.data.CategoryEntity(catId, "دسته موقت", 10, isDefault = false))

        val symId = "sym_in_temp_cat"
        dao.insertCustomSymbol(
            com.example.data.CustomSymbolEntity(symId, "نماد داخل دسته", categoryId = catId)
        )

        // Reassign and delete safely
        dao.reassignCategoryCustomSymbols(catId, "CUSTOM")
        dao.deleteCategory(catId)

        val updatedSym = dao.getAllCustomSymbols().first().find { it.id == symId }
        assertNotNull(updatedSym)
        assertEquals("CUSTOM", updatedSym?.categoryId)

        dao.deleteCustomSymbol(symId)
    }

    @Test
    fun testRoomMigration1To2_preservesProjectsAndCreatesTables() {
        val helper = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory()
        val config = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(ApplicationProvider.getApplicationContext<Context>())
            .name("migration_test.db")
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    db.execSQL("""
                        CREATE TABLE `projects` (
                            `id` TEXT NOT NULL, 
                            `title` TEXT NOT NULL, 
                            `author` TEXT NOT NULL, 
                            `createdAt` INTEGER NOT NULL, 
                            `updatedAt` INTEGER NOT NULL, 
                            `folderName` TEXT NOT NULL, 
                            `jsonContent` TEXT NOT NULL, 
                            PRIMARY KEY(`id`)
                        )
                    """.trimIndent())
                }

                override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val openHelper = helper.create(config)
        val db = openHelper.writableDatabase

        // Insert version 1 project
        db.execSQL("INSERT INTO `projects` VALUES ('proj_1', 'آزمون میگریشن', 'نویسنده', 1000, 2000, 'عمومی', '{}')")

        // Run Migration 1 -> 2
        com.example.data.AppDatabase.MIGRATION_1_2.migrate(db)

        // Verify project is intact
        val cursor = db.query("SELECT title, author FROM `projects` WHERE id = 'proj_1'")
        assertTrue(cursor.moveToFirst())
        assertEquals("آزمون میگریشن", cursor.getString(0))
        assertEquals("نویسنده", cursor.getString(1))
        cursor.close()

        // Verify new tables exist and can be inserted into
        db.execSQL("INSERT INTO `categories` (`id`, `titleFa`, `sortOrder`, `isDefault`) VALUES ('CAT1', 'دسته تست', 1, 0)")
        val catCursor = db.query("SELECT titleFa FROM `categories` WHERE id = 'CAT1'")
        assertTrue(catCursor.moveToFirst())
        assertEquals("دسته تست", catCursor.getString(0))
        catCursor.close()

        db.close()
    }

    @Test
    fun testPaperSizeA3AndA4DimensionsAreDistinctAndProportional() {
        // A4 logical & PDF points
        assertEquals(1000f, PaperSize.A4.widthPt, 0.1f)
        assertEquals(1414.28f, PaperSize.A4.heightPt, 0.1f)
        assertEquals(595, PaperSize.A4.pdfWidthPt)
        assertEquals(842, PaperSize.A4.pdfHeightPt)

        // A3 logical & PDF points must be distinct and larger
        assertEquals(1414.28f, PaperSize.A3.widthPt, 0.1f)
        assertEquals(2000f, PaperSize.A3.heightPt, 0.1f)
        assertEquals(842, PaperSize.A3.pdfWidthPt)
        assertEquals(1191, PaperSize.A3.pdfHeightPt)

        // Verify A3 is strictly larger than A4
        assertTrue(PaperSize.A3.widthPt > PaperSize.A4.widthPt)
        assertTrue(PaperSize.A3.heightPt > PaperSize.A4.heightPt)
        assertTrue(PaperSize.A3.pdfWidthPt > PaperSize.A4.pdfWidthPt)
        assertTrue(PaperSize.A3.pdfHeightPt > PaperSize.A4.pdfHeightPt)
    }

    @Test
    fun testCorruptProjectJsonSurfacedWithoutDropping() = kotlinx.coroutines.test.runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = com.example.data.AppDatabase.getInstance(context)
        val dao = db.projectDao()
        val repo = com.example.data.ScoreRepository(dao, context)

        val corruptId = "corrupt_test_id"
        val corruptEntity = com.example.data.ProjectEntity(
            id = corruptId,
            title = "پروژه مخدوش",
            author = "نامشخص",
            createdAt = 1000L,
            updatedAt = 2000L,
            folderName = "عمومی",
            jsonContent = "{ invalid: json content here !! }}"
        )
        dao.insertOrUpdateProject(corruptEntity)

        val all = repo.allProjects.first()
        val foundCorrupt = all.find { it.id == corruptId }
        assertNotNull(foundCorrupt)
        assertTrue(foundCorrupt?.isCorrupted == true)
        assertNotNull(foundCorrupt?.parseError)
        assertEquals("{ invalid: json content here !! }}", foundCorrupt?.rawCorruptJson)

        // Cleanup
        dao.deleteProject(corruptEntity)
    }

    @Test
    fun testMusicXmlOverflowWrapsToNextStaffOrPage() {
        val longMusicXml = buildString {
            append("""<?xml version="1.0" encoding="UTF-8"?>""")
            append("""<score-partwise version="3.1"><part-list><score-part id="P1"><part-name>Daf</part-name></score-part></part-list>""")
            append("""<part id="P1"><measure number="1">""")
            append("""<attributes><divisions>1</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>""")
            // Add 40 notes to exceed the right edge of staff 1 and trigger wrapping
            for (i in 1..40) {
                append("""<note><pitch><step>C</step><octave>4</octave></pitch><duration>1</duration><type>quarter</type></note>""")
                if (i % 4 == 0) {
                    append("""<barline location="right"><bar-style>regular</bar-style></barline>""")
                }
            }
            append("""</measure></part></score-partwise>""")
        }

        val result = ScoreImporter.parseMusicXml(longMusicXml)
        assertTrue(result.isSuccess)
        val project = result.data
        assertNotNull(project)
        assertTrue(project?.pages?.isNotEmpty() == true)
        val allSymbols = project?.pages?.flatMap { it.symbols } ?: emptyList()
        val notes = allSymbols.filter { it.type == SymbolType.NOTE_QUARTER }
        assertEquals(40, notes.size)

        // Verify that symbols wrap onto subsequent Y levels (different staves)
        val distinctYLevels = notes.map { it.y }.distinct()
        assertTrue("Expected notes to span across multiple staves/Y levels, got: ${distinctYLevels.size}", distinctYLevels.size >= 2)
    }

    @Test
    fun testMax16StavesSupportedOnPage() {
        val page16 = ScorePage(stavesCount = 16)
        val staves = page16.generateStaves()
        assertEquals(16, staves.size)
        // Ensure each staff has strictly descending/spaced lines
        for (i in 0 until 15) {
            assertTrue(staves[i].line4Y < staves[i + 1].line1Y)
        }
    }

    @Test
    fun testDeleteDefaultNoteDefinition_PersistsWithoutRecreation() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = db.projectDao()
        val repo = ScoreRepository(dao, context)

        // Ensure defaults are populated on first start
        repo.ensureDefaultNoteDefinitions()
        val initialDefs = dao.getAllNoteDefinitions().first()
        assertTrue(initialDefs.isNotEmpty())
        val wholeDef = initialDefs.find { it.id == "DEF_WHOLE" }
        assertNotNull("Default note DEF_WHOLE should exist initially", wholeDef)
        assertTrue(wholeDef?.isDefault == true)

        // Delete the default note definition permanently
        repo.deleteNoteDefinition("DEF_WHOLE")
        val afterDeletion = dao.getAllNoteDefinitions().first()
        val deletedCheck = afterDeletion.find { it.id == "DEF_WHOLE" }
        assertNull("DEF_WHOLE must be deleted from database", deletedCheck)

        // Simulate app restart: ensureDefaultNoteDefinitions called again
        repo.ensureDefaultNoteDefinitions()
        val afterRestart = dao.getAllNoteDefinitions().first()
        val restartedCheck = afterRestart.find { it.id == "DEF_WHOLE" }
        assertNull("DEF_WHOLE must remain permanently deleted and NOT be re-seeded", restartedCheck)

        db.close()
    }

    @Test
    fun testDeleteNoteDefinition_PreservesScoreSymbolsAndSharedImages() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = db.projectDao()
        val repo = ScoreRepository(dao, context)

        // Create a custom image file in filesDir
        val imagesDir = File(context.filesDir, "custom_notes").apply { mkdirs() }
        val testImageFile = File(imagesDir, "test_shared_note.png").apply {
            writeBytes(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47))
        }

        // Add a note definition using this image
        val def = repo.addNoteDefinition("نت ویژه تصویردار", SymbolType.CUSTOM_SYMBOL, "NOTES", "1/4", false, null)
        val defWithImage = def.copy(imagePath = testImageFile.absolutePath)
        dao.updateNoteDefinition(defWithImage)

        // Create a project score that references this image in a placed symbol
        val placedSymbol = ScoreSymbol(
            type = SymbolType.CUSTOM_SYMBOL,
            x = 200f,
            y = 300f,
            imagePath = testImageFile.absolutePath
        )
        val projectWithSymbol = ScoreProject(
            id = "proj_test_delete_safety",
            title = "پروژه حفظ نماد",
            pages = listOf(ScorePage(symbols = listOf(placedSymbol)))
        )
        repo.saveProject(projectWithSymbol)

        // Delete the note definition from library
        repo.deleteNoteDefinition(def.id)

        // 1. Definition must be removed from definition library
        val allDefs = dao.getAllNoteDefinitions().first()
        assertNull(allDefs.find { it.id == def.id })

        // 2. The physical image file must NOT be deleted because it is still referenced in the project score
        assertTrue("Image file must remain on disk while referenced by project scores", testImageFile.exists())

        // 3. The project score and its symbols must be intact
        val loadedProject = repo.allProjects.first().find { it.id == "proj_test_delete_safety" }
        assertNotNull(loadedProject)
        val loadedSymbol = loadedProject?.pages?.first()?.symbols?.first()
        assertEquals(placedSymbol.id, loadedSymbol?.id)
        assertEquals(testImageFile.absolutePath, loadedSymbol?.imagePath)

        db.close()
    }

    @Test
    fun testScreenToLogicalAndLogicalToScreenInversion() {
        val containerW = 1080f
        val containerH = 1920f
        val renderedW = 1000f
        val renderedH = 1414f
        val pageW = 1000f
        val pageH = 1414f
        val zoom = 2.5f
        val panX = 150f
        val panY = -80f

        val originalLogicalX = 450f
        val originalLogicalY = 720f

        // Convert logical to screen
        val (screenX, screenY) = ScoreTransformMath.logicalToScreen(
            logicalX = originalLogicalX,
            logicalY = originalLogicalY,
            containerWidthPx = containerW,
            containerHeightPx = containerH,
            renderedWidthPx = renderedW,
            renderedHeightPx = renderedH,
            pageW = pageW,
            pageH = pageH,
            zoomScale = zoom,
            panOffsetX = panX,
            panOffsetY = panY
        )

        // Convert screen back to logical
        val (recoveredLogicalX, recoveredLogicalY) = ScoreTransformMath.screenToLogical(
            screenX = screenX,
            screenY = screenY,
            containerWidthPx = containerW,
            containerHeightPx = containerH,
            renderedWidthPx = renderedW,
            renderedHeightPx = renderedH,
            pageW = pageW,
            pageH = pageH,
            zoomScale = zoom,
            panOffsetX = panX,
            panOffsetY = panY
        )

        assertEquals("Logical X must roundtrip exactly", originalLogicalX, recoveredLogicalX, 0.001f)
        assertEquals("Logical Y must roundtrip exactly", originalLogicalY, recoveredLogicalY, 0.001f)
    }

    @Test
    fun testZoomAnchorKeepsFocalPointStable() {
        val containerW = 1080f
        val containerH = 1920f
        val renderedW = 1000f
        val renderedH = 1414f
        val pageW = 1000f
        val pageH = 1414f

        val initialZoom = 1.0f
        val newZoom = 2.8f
        val initialPanX = 50f
        val initialPanY = -30f
        val centroidX = 600f
        val centroidY = 850f

        // Logical position under centroid before zoom
        val (beforeLogicalX, beforeLogicalY) = ScoreTransformMath.screenToLogical(
            screenX = centroidX,
            screenY = centroidY,
            containerWidthPx = containerW,
            containerHeightPx = containerH,
            renderedWidthPx = renderedW,
            renderedHeightPx = renderedH,
            pageW = pageW,
            pageH = pageH,
            zoomScale = initialZoom,
            panOffsetX = initialPanX,
            panOffsetY = initialPanY
        )

        // Calculate anchored pan offset for new zoom
        val (newPanX, newPanY) = ScoreTransformMath.calculateZoomAnchor(
            oldZoom = initialZoom,
            newZoom = newZoom,
            oldPanX = initialPanX,
            oldPanY = initialPanY,
            centroidX = centroidX,
            centroidY = centroidY,
            containerWidthPx = containerW,
            containerHeightPx = containerH
        )

        // Logical position under the same centroid after zoom with anchored pan
        val (afterLogicalX, afterLogicalY) = ScoreTransformMath.screenToLogical(
            screenX = centroidX,
            screenY = centroidY,
            containerWidthPx = containerW,
            containerHeightPx = containerH,
            renderedWidthPx = renderedW,
            renderedHeightPx = renderedH,
            pageW = pageW,
            pageH = pageH,
            zoomScale = newZoom,
            panOffsetX = newPanX,
            panOffsetY = newPanY
        )

        assertEquals("Point under fingers must not drift horizontally during zoom", beforeLogicalX, afterLogicalX, 0.01f)
        assertEquals("Point under fingers must not drift vertically during zoom", beforeLogicalY, afterLogicalY, 0.01f)
    }

    @Test
    fun testCategoryManagement_MoveSymbolsSafely() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        val dao = db.projectDao()
        val repo = ScoreRepository(dao, context)

        // Add source category and target category
        dao.insertCategory(CategoryEntity("CAT_SRC", "دسته مبدا", 1, false))
        dao.insertCategory(CategoryEntity("CAT_DST", "دسته مقصد", 2, false))

        // Add custom symbol in source category
        val sym = CustomSymbolEntity(id = "sym_move_test", name = "نماد انتقالی", categoryId = "CAT_SRC")
        dao.insertCustomSymbol(sym)

        // Delete source category and move symbols to target category
        repo.deleteCategoryAndMoveSymbols("CAT_SRC", "CAT_DST")

        // Check category deletion
        val categories = dao.getAllCategories().first()
        assertNull("Source category must be deleted", categories.find { it.id == "CAT_SRC" })
        assertNotNull("Target category must remain", categories.find { it.id == "CAT_DST" })

        // Check symbol was safely reassigned
        val symbols = dao.getAllCustomSymbols().first()
        val movedSym = symbols.find { it.id == "sym_move_test" }
        assertNotNull("Symbol must still exist", movedSym)
        assertEquals("Symbol must be in destination category", "CAT_DST", movedSym?.categoryId)

        db.close()
    }

    @Test
    fun testCategoryManagement_DeleteCategoryWithSymbols() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        val dao = db.projectDao()
        val repo = ScoreRepository(dao, context)

        dao.insertCategory(CategoryEntity("CAT_DROP", "دسته حذفی", 1, false))
        val sym = CustomSymbolEntity(id = "sym_to_delete", name = "نماد حذفی", categoryId = "CAT_DROP")
        dao.insertCustomSymbol(sym)

        // Delete category and all its symbols
        repo.deleteCategoryWithSymbols("CAT_DROP")

        val categories = dao.getAllCategories().first()
        assertNull("Category must be deleted", categories.find { it.id == "CAT_DROP" })

        val symbols = dao.getAllCustomSymbols().first()
        assertNull("Contained symbol must be deleted", symbols.find { it.id == "sym_to_delete" })

        db.close()
    }

    @Test
    fun testBulkSymbolImport_ZipInspectionAndImport() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        val dao = db.projectDao()
        val repo = ScoreRepository(dao, context)

        // 1. Create a valid test ZIP in cache
        val zipFile = File(context.cacheDir, "test_symbols.zip")
        val svgBytes = """
            <svg xmlns="http://www.w3.org/2000/svg" width="64" height="64" viewBox="0 0 64 64">
                <circle cx="32" cy="32" r="30" fill="black"/>
            </svg>
        """.trimIndent().toByteArray(Charsets.UTF_8)
        val manifestJson = """
            [
              {
                "name": "نماد تست زیپ",
                "image": "images/test.svg",
                "category": "CUSTOM",
                "isRingSound": true
              }
            ]
        """.trimIndent()

        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            zos.putNextEntry(ZipEntry("manifest.json"))
            zos.write(manifestJson.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("images/test.svg"))
            zos.write(svgBytes)
            zos.closeEntry()
        }

        // 2. Inspect ZIP
        val previewResult = BulkSymbolImporter.inspectZip(
            context = context,
            zipUri = android.net.Uri.fromFile(zipFile),
            existingCustomSymbols = emptyList(),
            availableCategories = listOf(CategoryEntity("CUSTOM", "سفارشی", 1, true))
        )

        assertTrue("ZIP inspection must succeed", previewResult.isSuccess)
        val preview = previewResult.getOrNull()
        assertNotNull(preview)
        assertEquals(1, preview?.totalFound)
        assertEquals(1, preview?.validItems?.size)
        assertEquals("نماد تست زیپ", preview?.validItems?.first()?.name)
        assertTrue(preview?.validItems?.first()?.isRingSound == true)

        // 3. Execute bulk import
        val importedCount = BulkSymbolImporter.executeImport(
            context = context,
            itemsToImport = preview!!.validItems,
            renameDuplicates = false
        ) { entity ->
            repo.insertCustomSymbol(entity)
        }

        assertEquals(1, importedCount)
        val savedSymbols = dao.getAllCustomSymbols().first()
        val imported = savedSymbols.find { it.name == "نماد تست زیپ" }
        assertNotNull("Imported symbol must be in database", imported)
        assertTrue("Imported image must exist on disk", File(imported!!.imagePath!!).exists())

        db.close()
    }
}

