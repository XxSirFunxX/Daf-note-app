package com.example.io

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.model.PaperSize
import com.example.model.ScorePage
import com.example.model.ScoreProject
import com.example.render.ScoreRenderer
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

object ScoreExporter {

    /**
     * Exports all pages of a project to a standard multi-page PDF document.
     */
    fun exportToPdf(context: Context, project: ScoreProject): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val fileName = "score_${project.title.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
        val pdfFile = File(exportDir, fileName)

        val document = PdfDocument()

        for ((index, page) in project.pages.withIndex()) {
            val width = page.paperSize.widthPt.toInt()
            val height = page.paperSize.heightPt.toInt()

            val pageInfo = PdfDocument.PageInfo.Builder(width, height, index + 1).create()
            val pdfPage = document.startPage(pageInfo)

            ScoreRenderer.drawPage(
                canvas = pdfPage.canvas,
                page = page,
                selectedSymbolIds = emptySet(),
                showGuides = false,
                highlightLine4 = false
            )

            document.finishPage(pdfPage)
        }

        FileOutputStream(pdfFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        return pdfFile
    }

    /**
     * Exports a single page as a high-resolution PNG or JPG image.
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
     * Converts a ScoreProject into a structured JSON string.
     */
    fun projectToJson(project: ScoreProject): String {
        val root = JSONObject()
        root.put("version", project.version)
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
                symbolsArray.put(symObj)
            }
            pageObj.put("symbols", symbolsArray)
            pagesArray.put(pageObj)
        }
        root.put("pages", pagesArray)

        return root.toString(2)
    }

    /**
     * Exports the project as a downloadable .chaharkhat or .json file.
     */
    fun exportProjectFile(context: Context, project: ScoreProject): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val fileName = "${project.title.replace(" ", "_")}.chaharkhat"
        val file = File(exportDir, fileName)
        file.writeText(projectToJson(project))
        return file
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
