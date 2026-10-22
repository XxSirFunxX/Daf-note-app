package com.example.io

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.model.PaperSize
import com.example.model.ScorePage
import com.example.model.ScoreProject
import com.example.model.ScoreSymbol
import com.example.render.ScoreRenderer
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object ScoreExporter {

    const val EXTENSION_DAFNT = "dafnt"
    const val EXTENSION_LEGACY = "chaharkhat"

    /**
     * Converts a ScoreProject into a structured JSON string.
     */
    fun projectToJson(project: ScoreProject): String {
        val root = JSONObject()
        root.put("version", 2)
        root.put("format", "DAFNT")
        root.put("id", project.id)
        root.put("title", project.title)
        root.put("author", project.author)
        root.put("createdAt", project.createdAt)
        root.put("updatedAt", project.updatedAt)
        root.put("folderId", project.folderId ?: "")
        root.put("folderName", project.folderName ?: "")
        root.put("activePageIndex", project.activePageIndex)

        val pagesArray = JSONArray()
        for (page in project.pages) {
            val pageObj = JSONObject()
            pageObj.put("id", page.id)
            pageObj.put("pageNumber", page.pageNumber)
            pageObj.put("title", page.title)
            pageObj.put("subtitle", page.subtitle)
            pageObj.put("lessonNumber", page.lessonNumber)
            pageObj.put("paperSize", page.paperSize.name)
            pageObj.put("stavesCount", page.stavesCount)
            pageObj.put("lineSpacing", page.lineSpacing.toDouble())
            pageObj.put("line4SpacingRatio", page.line4SpacingRatio.toDouble())
            pageObj.put("marginTop", page.marginTop.toDouble())
            pageObj.put("marginBottom", page.marginBottom.toDouble())
            pageObj.put("marginLeft", page.marginLeft.toDouble())
            pageObj.put("marginRight", page.marginRight.toDouble())

            val symbolsArray = JSONArray()
            for (sym in page.symbols) {
                val symObj = JSONObject()
                symObj.put("id", sym.id)
                symObj.put("type", sym.type.name)
                symObj.put("category", sym.category.name)
                symObj.put("name", sym.name)
                symObj.put("x", sym.x.toDouble())
                symObj.put("y", sym.y.toDouble())
                symObj.put("scale", sym.scale.toDouble())
                symObj.put("rotation", sym.rotation.toDouble())
                symObj.put("isStemUp", sym.isStemUp)
                symObj.put("isRingSound", sym.isRingSound)
                symObj.put("textValue", sym.textValue ?: "")
                symObj.put("customData", sym.customData ?: "")
                symObj.put("colorHex", sym.colorHex)
                symObj.put("imagePath", sym.imagePath ?: "")
                symObj.put("durationValue", sym.durationValue ?: "")
                symbolsArray.put(symObj)
            }
            pageObj.put("symbols", symbolsArray)
            pagesArray.put(pageObj)
        }
        root.put("pages", pagesArray)

        return root.toString(2)
    }

    /**
     * Exports the project as a downloadable .dafnt file in cache.
     */
    fun exportProjectFile(context: Context, project: ScoreProject): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val fileName = "${project.title.replace(" ", "_")}.$EXTENSION_DAFNT"
        val file = File(exportDir, fileName)
        file.writeText(projectToJson(project))
        return file
    }

    /**
     * Writes the project JSON directly to a user-selected URI (Storage Access Framework).
     */
    fun writeProjectToUri(context: Context, uri: Uri, project: ScoreProject): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(projectToJson(project).toByteArray(Charsets.UTF_8))
                out.flush()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Exports all pages of a project to a standard multi-page PDF document in cache.
     */
    fun exportToPdf(context: Context, project: ScoreProject): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val fileName = "score_${project.title.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
        val pdfFile = File(exportDir, fileName)

        val document = PdfDocument()

        for ((index, page) in project.pages.withIndex()) {
            val pdfWidth = page.paperSize.pdfWidthPt
            val pdfHeight = page.paperSize.pdfHeightPt

            val pageInfo = PdfDocument.PageInfo.Builder(pdfWidth, pdfHeight, index + 1).create()
            val pdfPage = document.startPage(pageInfo)

            val scaleX = pdfWidth.toFloat() / page.paperSize.widthPt
            val scaleY = pdfHeight.toFloat() / page.paperSize.heightPt
            pdfPage.canvas.save()
            pdfPage.canvas.scale(scaleX, scaleY)

            ScoreRenderer.drawPage(
                canvas = pdfPage.canvas,
                page = page,
                selectedSymbolIds = emptySet(),
                showGuides = false,
                highlightLine4 = false
            )
            pdfPage.canvas.restore()

            document.finishPage(pdfPage)
        }

        FileOutputStream(pdfFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        return pdfFile
    }

    /**
     * Writes PDF directly to a user-selected destination URI.
     */
    fun writePdfToUri(context: Context, uri: Uri, project: ScoreProject): Boolean {
        return try {
            val document = PdfDocument()
            for ((index, page) in project.pages.withIndex()) {
                val pdfWidth = page.paperSize.pdfWidthPt
                val pdfHeight = page.paperSize.pdfHeightPt

                val pageInfo = PdfDocument.PageInfo.Builder(pdfWidth, pdfHeight, index + 1).create()
                val pdfPage = document.startPage(pageInfo)

                val scaleX = pdfWidth.toFloat() / page.paperSize.widthPt
                val scaleY = pdfHeight.toFloat() / page.paperSize.heightPt
                pdfPage.canvas.save()
                pdfPage.canvas.scale(scaleX, scaleY)

                ScoreRenderer.drawPage(
                    canvas = pdfPage.canvas,
                    page = page,
                    selectedSymbolIds = emptySet(),
                    showGuides = false,
                    highlightLine4 = false
                )
                pdfPage.canvas.restore()

                document.finishPage(pdfPage)
            }

            context.contentResolver.openOutputStream(uri)?.use { out ->
                document.writeTo(out)
            }
            document.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Exports a single page as a high-resolution PNG or JPG image file in cache.
     */
    fun exportPageToImage(
        context: Context,
        page: ScorePage,
        isPng: Boolean = true,
        quality: Int = 95
    ): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val ext = if (isPng) "png" else "jpg"
        val fileName = "page_${page.pageNumber}_${System.currentTimeMillis()}.$ext"
        val imageFile = File(exportDir, fileName)

        val width = page.paperSize.widthPt.toInt()
        val height = page.paperSize.heightPt.toInt()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        ScoreRenderer.drawPage(
            canvas = canvas,
            page = page,
            selectedSymbolIds = emptySet(),
            showGuides = false,
            highlightLine4 = false
        )

        val compressFormat = if (isPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
        FileOutputStream(imageFile).use { out ->
            bitmap.compress(compressFormat, quality, out)
        }
        bitmap.recycle()

        return imageFile
    }

    /**
     * Writes image directly to a user-selected destination URI.
     */
    fun writeImageToUri(
        context: Context,
        uri: Uri,
        page: ScorePage,
        isPng: Boolean = true,
        quality: Int = 95
    ): Boolean {
        return try {
            val width = page.paperSize.widthPt.toInt()
            val height = page.paperSize.heightPt.toInt()

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            ScoreRenderer.drawPage(
                canvas = canvas,
                page = page,
                selectedSymbolIds = emptySet(),
                showGuides = false,
                highlightLine4 = false
            )

            val compressFormat = if (isPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
            context.contentResolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(compressFormat, quality, out)
            }
            bitmap.recycle()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Writes project JSON/dafnt directly to a user-selected destination URI.
     */
    fun writeProjectToUri(context: Context, uri: Uri, project: ScoreProject): Boolean {
        return try {
            val json = projectToJson(project)
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.bufferedWriter(Charsets.UTF_8).use { it.write(json) }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Helper to share any generated file via Android's native share sheet.
     */
    fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }
}
