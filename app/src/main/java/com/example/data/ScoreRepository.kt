package com.example.data

import com.example.io.ScoreExporter
import com.example.io.ScoreImporter
import com.example.model.PaperSize
import com.example.model.ScorePage
import com.example.model.ScoreProject
import com.example.model.ScoreSymbol
import com.example.model.SymbolType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ScoreRepository(private val dao: ProjectDao) {

    val allProjects: Flow<List<ScoreProject>> = dao.getAllProjects().map { list ->
        list.mapNotNull { entity ->
            ScoreImporter.parseProjectJson(entity.jsonContent).data
        }
    }

    val allFolders: Flow<List<FolderEntity>> = dao.getAllFolders()
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
        dao.insertFolder(FolderEntity(id = java.util.UUID.randomUUID().toString(), name = name))
    }

    suspend fun addCustomSymbol(name: String, isRingSound: Boolean, vectorData: String?) {
        dao.insertCustomSymbol(
            CustomSymbolEntity(
                id = java.util.UUID.randomUUID().toString(),
                name = name,
                isRingSound = isRingSound,
                vectorData = vectorData
            )
        )
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
            // Beat 1 label
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
            // Bar line 1
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
            // Bar line 2
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
            // Bar line 3
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
                    y = staff1.line4Y, // Constrained to line 4
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
            // Final bar line
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
