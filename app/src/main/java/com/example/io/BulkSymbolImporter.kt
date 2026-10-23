package com.example.io

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.data.CategoryEntity
import com.example.data.CustomSymbolEntity
import com.example.util.CustomSymbolImageHelper
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

data class BulkImportItem(
    val name: String,
    val categoryId: String,
    val isRingSound: Boolean,
    val imageBytes: ByteArray,
    val isSvg: Boolean,
    val fileName: String,
    val previewBitmap: Bitmap?,
    val isDuplicate: Boolean = false,
    val validationError: String? = null
)

data class BulkImportPreview(
    val totalFound: Int,
    val validItems: List<BulkImportItem>,
    val invalidItems: List<BulkImportItem>,
    val duplicateCount: Int
)

object BulkSymbolImporter {

    const val MAX_ZIP_TOTAL_UNCOMPRESSED_BYTES = 50 * 1024 * 1024 // 50MB safety limit

    fun inspectZip(
        context: Context,
        zipUri: Uri,
        existingCustomSymbols: List<CustomSymbolEntity>,
        availableCategories: List<CategoryEntity>
    ): Result<BulkImportPreview> {
        return runCatching {
            val entriesMap = mutableMapOf<String, ByteArray>()
            var manifestContent: String? = null
            var totalUncompressedSize = 0L

            context.contentResolver.openInputStream(zipUri)?.use { rawStream ->
                ZipInputStream(rawStream).use { zipStream ->
                    var entry: ZipEntry? = zipStream.nextEntry
                    while (entry != null) {
                        val name = entry.name.replace("\\", "/")
                        // Check for Zip Slip directory traversal vulnerability
                        if (name.contains("../") || name.startsWith("/")) {
                            throw SecurityException("فایل ZIP حاوی مسیرهای غیرمجاز است.")
                        }

                        if (!entry.isDirectory) {
                            val bytes = zipStream.readBytes()
                            totalUncompressedSize += bytes.size
                            if (totalUncompressedSize > MAX_ZIP_TOTAL_UNCOMPRESSED_BYTES) {
                                throw IllegalStateException("حجم داده‌های داخل فایل فشرده بیش از حد مجاز است.")
                            }

                            val normalizedName = name.trimStart('/')
                            entriesMap[normalizedName] = bytes

                            val baseName = File(normalizedName).name.lowercase()
                            if (baseName == "symbols.json" || baseName == "manifest.json") {
                                manifestContent = bytes.toString(Charsets.UTF_8)
                            }
                        }
                        zipStream.closeEntry()
                        entry = zipStream.nextEntry
                    }
                }
            } ?: return Result.failure(IllegalArgumentException("امکان باز کردن فایل ZIP وجود ندارد."))

            if (manifestContent == null) {
                // If neither symbols.json nor manifest.json was explicitly named, look for any .json file
                val jsonEntry = entriesMap.entries.firstOrNull { it.key.endsWith(".json", ignoreCase = true) }
                if (jsonEntry != null) {
                    manifestContent = jsonEntry.value.toString(Charsets.UTF_8)
                } else {
                    return Result.failure(IllegalArgumentException("فایل manifest.json یا symbols.json در فایل ZIP یافت نشد."))
                }
            }

            val validItems = mutableListOf<BulkImportItem>()
            val invalidItems = mutableListOf<BulkImportItem>()

            val symbolsArray = try {
                val trimmed = manifestContent!!.trim()
                if (trimmed.startsWith("[")) {
                    JSONArray(trimmed)
                } else {
                    val root = JSONObject(trimmed)
                    root.optJSONArray("symbols") ?: JSONArray()
                }
            } catch (e: Exception) {
                return Result.failure(IllegalArgumentException("فرمت فایل JSON نامعتبر است: ${e.localizedMessage}"))
            }

            val categoryMap = availableCategories.associateBy { it.id.uppercase() }
            val categoryTitleMap = availableCategories.associateBy { it.titleFa.trim() }

            for (i in 0 until symbolsArray.length()) {
                val symObj = symbolsArray.optJSONObject(i)
                if (symObj == null) {
                    invalidItems.add(
                        BulkImportItem(
                            name = "مورد نامعتبر #${i + 1}",
                            categoryId = "CUSTOM",
                            isRingSound = false,
                            imageBytes = byteArrayOf(),
                            isSvg = false,
                            fileName = "",
                            previewBitmap = null,
                            validationError = "ساختار تعریف نماد در JSON نامعتبر است."
                        )
                    )
                    continue
                }

                val symName = symObj.optString("name", "").trim()
                val imageRelPath = symObj.optString("image", "").trim().replace("\\", "/").trimStart('/')
                val categoryParam = symObj.optString("category", "CUSTOM").trim()
                val isRingSound = symObj.optBoolean("isRingSound", false)

                if (symName.isBlank()) {
                    invalidItems.add(
                        BulkImportItem(
                            name = "نماد بدون نام #${i + 1}",
                            categoryId = "CUSTOM",
                            isRingSound = isRingSound,
                            imageBytes = byteArrayOf(),
                            isSvg = false,
                            fileName = imageRelPath,
                            previewBitmap = null,
                            validationError = "نام نماد نمی‌تواند خالی باشد."
                        )
                    )
                    continue
                }

                // Match category ID or Persian title
                val mappedCategory = categoryMap[categoryParam.uppercase()]?.id
                    ?: categoryTitleMap[categoryParam]?.id
                    ?: "CUSTOM"

                // Locate image in zip entries (exact or filename match)
                val imageBytes = entriesMap[imageRelPath]
                    ?: entriesMap.entries.firstOrNull { File(it.key).name.equals(File(imageRelPath).name, ignoreCase = true) }?.value

                if (imageBytes == null || imageBytes.isEmpty()) {
                    invalidItems.add(
                        BulkImportItem(
                            name = symName,
                            categoryId = mappedCategory,
                            isRingSound = isRingSound,
                            imageBytes = byteArrayOf(),
                            isSvg = false,
                            fileName = imageRelPath,
                            previewBitmap = null,
                            validationError = "فایل تصویر '$imageRelPath' در آرشیو ZIP یافت نشد."
                        )
                    )
                    continue
                }

                val mime = if (imageRelPath.endsWith(".svg", ignoreCase = true)) "image/svg+xml" else null
                val validation = CustomSymbolImageHelper.validateImage(imageBytes, mime)
                if (validation is CustomSymbolImageHelper.ValidationResult.Error) {
                    invalidItems.add(
                        BulkImportItem(
                            name = symName,
                            categoryId = mappedCategory,
                            isRingSound = isRingSound,
                            imageBytes = imageBytes,
                            isSvg = false,
                            fileName = imageRelPath,
                            previewBitmap = null,
                            validationError = validation.messageFa
                        )
                    )
                    continue
                }

                val isSvg = (validation as CustomSymbolImageHelper.ValidationResult.Success).isSvg
                val previewBitmap = if (isSvg) {
                    CustomSymbolImageHelper.renderSvgToBitmap(imageBytes.inputStream(), 96, 96)
                } else {
                    BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                }

                val isDuplicate = existingCustomSymbols.any {
                    it.name.trim().equals(symName, ignoreCase = true) && it.categoryId == mappedCategory
                }

                validItems.add(
                    BulkImportItem(
                        name = symName,
                        categoryId = mappedCategory,
                        isRingSound = isRingSound,
                        imageBytes = imageBytes,
                        isSvg = isSvg,
                        fileName = imageRelPath,
                        previewBitmap = previewBitmap,
                        isDuplicate = isDuplicate
                    )
                )
            }

            val duplicateCount = validItems.count { it.isDuplicate }
            BulkImportPreview(
                totalFound = symbolsArray.length(),
                validItems = validItems,
                invalidItems = invalidItems,
                duplicateCount = duplicateCount
            )
        }
    }

    suspend fun executeImport(
        context: Context,
        itemsToImport: List<BulkImportItem>,
        renameDuplicates: Boolean,
        onInsertEntity: suspend (CustomSymbolEntity) -> Unit
    ): Int {
        val symbolsDir = File(context.filesDir, "symbols").apply { mkdirs() }
        var importedCount = 0

        for (item in itemsToImport) {
            val finalName = if (renameDuplicates && item.isDuplicate) {
                "${item.name} (کپی)"
            } else {
                item.name
            }

            val id = UUID.randomUUID().toString()
            val ext = if (item.isSvg) "svg" else "png"
            val destFile = File(symbolsDir, "sym_$id.$ext")

            try {
                FileOutputStream(destFile).use { it.write(item.imageBytes) }
                val entity = CustomSymbolEntity(
                    id = id,
                    name = finalName,
                    categoryId = item.categoryId,
                    isRingSound = item.isRingSound,
                    imagePath = destFile.absolutePath,
                    createdAt = System.currentTimeMillis()
                )
                onInsertEntity(entity)
                importedCount++
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return importedCount
    }
}
