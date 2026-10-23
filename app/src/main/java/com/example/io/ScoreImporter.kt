package com.example.io

import android.content.Context
import android.net.Uri
import com.example.model.PaperSize
import com.example.model.ScorePage
import com.example.model.ScoreProject
import com.example.model.ScoreSymbol
import com.example.model.SymbolCategory
import com.example.model.SymbolType
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.io.StringReader

data class MusicXmlImportReport(
    val importedNotesCount: Int,
    val importedRestsCount: Int,
    val detectedTimeSignature: String,
    val incompatibleElements: List<String>
)

data class ImportResult<T>(
    val data: T?,
    val isSuccess: Boolean,
    val errorMessage: String? = null,
    val report: MusicXmlImportReport? = null
)

object ScoreImporter {

    /**
     * Reads and parses a project from a SAF content Uri or File Uri.
     */
    fun readProjectFromUri(context: Context, uri: Uri): ImportResult<ScoreProject> {
        return try {
            val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).readText()
            } ?: return ImportResult(null, false, "خطا در خواندن فایل از مسیر انتخاب‌شده")

            val trimmed = content.trimStart()
            val isXml = trimmed.startsWith("<?xml", ignoreCase = true) || 
                        trimmed.startsWith("<score-partwise", ignoreCase = true) ||
                        trimmed.startsWith("<!DOCTYPE score-partwise", ignoreCase = true)
            val isJson = trimmed.startsWith("{") || trimmed.startsWith("[")

            if (isXml) {
                parseMusicXml(content)
            } else if (isJson) {
                parseProjectJson(content)
            } else {
                ImportResult(null, false, "فرمت فایل ناشناخته است. لطفاً فایل معتبر .dafnt یا MusicXML انتخاب کنید.")
            }
        } catch (e: Exception) {
            ImportResult(null, false, "خطا در بارگذاری فایل: ${e.localizedMessage}")
        }
    }

    /**
     * Parses native project JSON (.dafnt or legacy .chaharkhat) into a ScoreProject.
     */
    fun parseProjectJson(jsonString: String): ImportResult<ScoreProject> {
        return try {
            val root = JSONObject(jsonString)
            val version = root.optInt("version", 1)
            val id = root.optString("id", java.util.UUID.randomUUID().toString())
            val title = root.optString("title", "پروژه وارد شده")
            val author = root.optString("author", "هنرمند")
            val createdAt = root.optLong("createdAt", System.currentTimeMillis())
            val updatedAt = root.optLong("updatedAt", System.currentTimeMillis())
            val folderId = root.optString("folderId").ifEmpty { null }
            val folderName = root.optString("folderName").ifEmpty { null }
            val activePageIndex = root.optInt("activePageIndex", 0)

            val pagesArray = root.getJSONArray("pages")
            val pagesList = mutableListOf<ScorePage>()

            for (i in 0 until pagesArray.length()) {
                val pageObj = pagesArray.getJSONObject(i)
                val paperSizeName = pageObj.optString("paperSize", "A4")
                val paperSize = try { PaperSize.valueOf(paperSizeName) } catch (e: Exception) { PaperSize.A4 }

                val symbolsArray = pageObj.optJSONArray("symbols")
                val symbolList = mutableListOf<ScoreSymbol>()

                if (symbolsArray != null) {
                    for (s in 0 until symbolsArray.length()) {
                        val symObj = symbolsArray.getJSONObject(s)
                        val typeName = symObj.getString("type")
                        val symType = try { SymbolType.valueOf(typeName) } catch (e: Exception) { SymbolType.NOTE_QUARTER }
                        val catName = symObj.optString("category", symType.defaultCategory.name)
                        val category = try { SymbolCategory.valueOf(catName) } catch (e: Exception) { symType.defaultCategory }

                        symbolList.add(
                            ScoreSymbol(
                                id = symObj.optString("id", java.util.UUID.randomUUID().toString()),
                                type = symType,
                                category = category,
                                name = symObj.optString("name", symType.defaultTitleFa),
                                x = symObj.getDouble("x").toFloat(),
                                y = symObj.getDouble("y").toFloat(),
                                scale = symObj.optDouble("scale", 1.0).toFloat(),
                                rotation = symObj.optDouble("rotation", 0.0).toFloat(),
                                isStemUp = symObj.optBoolean("isStemUp", true),
                                isRingSound = symObj.optBoolean("isRingSound", category == SymbolCategory.RING_SOUNDS || symType.isRingSound),
                                textValue = symObj.optString("textValue").ifEmpty { null },
                                customData = symObj.optString("customData").ifEmpty { null },
                                colorHex = symObj.optString("colorHex", "#1E1C1A"),
                                imagePath = symObj.optString("imagePath").ifEmpty { null },
                                durationValue = symObj.optString("durationValue").ifEmpty { null }
                            )
                        )
                    }
                }

                pagesList.add(
                    ScorePage(
                        id = pageObj.optString("id", java.util.UUID.randomUUID().toString()),
                        pageNumber = pageObj.optInt("pageNumber", i + 1),
                        title = pageObj.optString("title", title),
                        subtitle = pageObj.optString("subtitle", ""),
                        lessonNumber = pageObj.optString("lessonNumber", "۱"),
                        paperSize = paperSize,
                        stavesCount = pageObj.optInt("stavesCount", 5).coerceIn(1, 16),
                        lineSpacing = pageObj.optDouble("lineSpacing", 16.0).toFloat(),
                        line4SpacingRatio = pageObj.optDouble("line4SpacingRatio", 1.0).toFloat(),
                        marginTop = pageObj.optDouble("marginTop", 110.0).toFloat(),
                        marginBottom = pageObj.optDouble("marginBottom", 80.0).toFloat(),
                        marginLeft = pageObj.optDouble("marginLeft", 60.0).toFloat(),
                        marginRight = pageObj.optDouble("marginRight", 60.0).toFloat(),
                        symbols = symbolList
                    )
                )
            }

            ImportResult(
                data = ScoreProject(
                    id = id,
                    title = title,
                    author = author,
                    createdAt = createdAt,
                    updatedAt = updatedAt,
                    folderId = folderId,
                    folderName = folderName,
                    pages = if (pagesList.isEmpty()) listOf(ScorePage()) else pagesList,
                    activePageIndex = activePageIndex,
                    version = version
                ),
                isSuccess = true
            )
        } catch (e: Exception) {
            ImportResult(
                data = null,
                isSuccess = false,
                errorMessage = "خطا در پردازش فایل پروژه: فرمت داده نامعتبر است (${e.localizedMessage})"
            )
        }
    }

    /**
     * Parses MusicXML and converts recognized notes and rests to the 4-line staff system,
     * while compiling a transparent report of unmapped parameters.
     */
    fun parseMusicXml(xmlContent: String): ImportResult<ScoreProject> {
        return try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xmlContent))

            var eventType = parser.eventType
            var currentTag = ""
            var beats = "2"
            var beatType = "4"

            var isNote = false
            var isRest = false
            var noteTypeStr = ""
            var step = "C"
            var octave = 4

            var notesCount = 0
            var restsCount = 0
            val unmappedItems = mutableListOf<String>()

            val templatePage = ScorePage(title = "پروژه وارد شده از MusicXML")
            var currentStaves = templatePage.generateStaves()
            if (currentStaves.isEmpty()) return ImportResult(null, false, "خطا در ساخت حامل")

            val pages = mutableListOf<ScorePage>()
            val currentSymbols = mutableListOf<ScoreSymbol>()
            var currentStaffIndex = 0
            var currentStaff = currentStaves[currentStaffIndex]
            var currentX = currentStaff.left + 75f
            val xStep = 45f

            // Add time signature symbol on the first staff
            currentSymbols.add(
                ScoreSymbol(
                    type = SymbolType.TIME_CUSTOM,
                    x = currentStaff.left + 35f,
                    y = (currentStaff.line1Y + currentStaff.line4Y) / 2f,
                    textValue = "$beats/$beatType"
                )
            )

            fun advanceStaffOrPage() {
                currentStaffIndex++
                if (currentStaffIndex < currentStaves.size) {
                    currentStaff = currentStaves[currentStaffIndex]
                    currentX = currentStaff.left + 50f
                } else {
                    // Current page is full, create a new page
                    pages.add(
                        templatePage.copy(
                            id = java.util.UUID.randomUUID().toString(),
                            pageNumber = pages.size + 1,
                            symbols = currentSymbols.toList()
                        )
                    )
                    currentSymbols.clear()
                    currentStaffIndex = 0
                    currentStaves = templatePage.generateStaves()
                    currentStaff = currentStaves[0]
                    currentX = currentStaff.left + 50f
                }
            }

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name
                        when (currentTag) {
                            "beats" -> beats = parser.nextText()
                            "beat-type" -> beatType = parser.nextText()
                            "note" -> {
                                isNote = true
                                isRest = false
                                noteTypeStr = "quarter"
                                step = "C"
                                octave = 4
                            }
                            "rest" -> isRest = true
                            "type" -> noteTypeStr = parser.nextText()
                            "step" -> step = parser.nextText()
                            "octave" -> octave = parser.nextText().toIntOrNull() ?: 4
                            "barline" -> {
                                currentSymbols.add(
                                    ScoreSymbol(
                                        type = SymbolType.BAR_SINGLE,
                                        x = currentX,
                                        y = (currentStaff.line1Y + currentStaff.line4Y) / 2f,
                                        isRingSound = false
                                    )
                                )
                                currentX += 28f
                                if (currentX > currentStaff.right - 45f) {
                                    advanceStaffOrPage()
                                }
                            }
                            "key", "clef", "dynamics", "lyric" -> {
                                val name = currentTag
                                if (!unmappedItems.contains(name)) {
                                    unmappedItems.add(name)
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "note") {
                            val symbolType: SymbolType = if (isRest) {
                                restsCount++
                                when (noteTypeStr) {
                                    "whole" -> SymbolType.REST_WHOLE
                                    "half" -> SymbolType.REST_HALF
                                    "quarter" -> SymbolType.REST_QUARTER
                                    "eighth" -> SymbolType.REST_EIGHTH
                                    "16th" -> SymbolType.REST_SIXTEENTH
                                    "32nd" -> SymbolType.REST_THIRTYSECOND
                                    else -> SymbolType.REST_QUARTER
                                }
                            } else {
                                notesCount++
                                when (noteTypeStr) {
                                    "whole" -> SymbolType.NOTE_WHOLE
                                    "half" -> SymbolType.NOTE_HALF
                                    "quarter" -> SymbolType.NOTE_QUARTER
                                    "eighth" -> SymbolType.NOTE_EIGHTH
                                    "16th" -> SymbolType.NOTE_SIXTEENTH
                                    "32nd" -> SymbolType.NOTE_THIRTYSECOND
                                    else -> SymbolType.NOTE_QUARTER
                                }
                            }

                            val targetY = when {
                                isRest -> currentStaff.line2Y
                                octave <= 3 -> currentStaff.line1Y
                                octave == 4 && (step == "C" || step == "D" || step == "E") -> currentStaff.line2Y
                                octave == 4 -> currentStaff.line3Y
                                else -> currentStaff.line4Y
                            }

                            currentSymbols.add(
                                ScoreSymbol(
                                    type = symbolType,
                                    x = currentX,
                                    y = targetY,
                                    isRingSound = false
                                )
                            )

                            currentX += xStep
                            if (currentX > currentStaff.right - 45f) {
                                advanceStaffOrPage()
                            }
                            isNote = false
                        }
                    }
                }
                eventType = parser.next()
            }

            if (currentSymbols.isNotEmpty() || pages.isEmpty()) {
                pages.add(
                    templatePage.copy(
                        id = java.util.UUID.randomUUID().toString(),
                        pageNumber = pages.size + 1,
                        symbols = currentSymbols.toList()
                    )
                )
            }

            val generatedProject = ScoreProject(
                title = "MusicXML ($beats/$beatType)",
                pages = pages
            )

            val persianIncompatibles = unmappedItems.map { tag ->
                when (tag) {
                    "clef" -> "کلید پنج‌خطی استاندارد (به حامل ۴خطی تبدیل شد)"
                    "key" -> "سرکلید دیز/بمل (سیستم ۴خطی بدون سرکلید غربی است)"
                    "lyric" -> "شعر/متن ترانه"
                    "dynamics" -> "دینامیک‌های پیوسته چندصدایی"
                    else -> "عنصر موسیقی: $tag"
                }
            }

            ImportResult(
                data = generatedProject,
                isSuccess = true,
                report = MusicXmlImportReport(
                    importedNotesCount = notesCount,
                    importedRestsCount = restsCount,
                    detectedTimeSignature = "$beats/$beatType",
                    incompatibleElements = persianIncompatibles
                )
            )
        } catch (e: Exception) {
            ImportResult(
                data = null,
                isSuccess = false,
                errorMessage = "خطا در خواندن فایل MusicXML: ${e.localizedMessage}"
            )
        }
    }
}
