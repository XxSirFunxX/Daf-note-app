package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.net.Uri
import android.util.LruCache
import com.caverock.androidsvg.SVG
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream

/**
 * Robust image utility for custom musical symbols.
 * Handles PNG, JPEG, WebP, and SVG images with full validation and error reporting.
 */
object CustomSymbolImageHelper {

    const val MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024 // 5 MB
    const val MAX_IMAGE_DIMENSION = 4096
    const val SUPPORTED_FORMATS_DESC = "فرمت‌های مجاز: PNG، SVG، JPG، WebP (حداکثر حجم ۵ مگابایت)"

    sealed class ValidationResult {
        data class Success(val format: String, val isSvg: Boolean, val width: Int = 0, val height: Int = 0) : ValidationResult()
        data class Error(val messageFa: String) : ValidationResult()
    }

    /**
     * Inspects the input stream/bytes and validates whether it's a valid PNG, JPEG, WebP, or SVG.
     */
    fun validateImage(bytes: ByteArray, declaredMimeType: String?): ValidationResult {
        if (bytes.isEmpty()) {
            return ValidationResult.Error("فایل انتخاب شده خالی است.")
        }

        if (bytes.size > MAX_FILE_SIZE_BYTES) {
            val sizeMb = String.format(java.util.Locale.US, "%.1f", bytes.size / (1024f * 1024f))
            return ValidationResult.Error("حجم فایل انتخابی ($sizeMb مگابایت) بیش از حد مجاز است. حداکثر حجم مجاز ۵ مگابایت می‌باشد.")
        }

        // Check for non-image MIME types (e.g. text/plain, application/pdf, audio/...)
        if (declaredMimeType != null && 
            !declaredMimeType.startsWith("image/") && 
            declaredMimeType != "application/octet-stream" &&
            !declaredMimeType.contains("svg")
        ) {
            return ValidationResult.Error("فرمت فایل ($declaredMimeType) پشتیبانی نمی‌شود. لطفاً فایل تصویری انتخاب کنید.")
        }

        // 1. Check for SVG
        val isLikelySvg = declaredMimeType?.contains("svg", ignoreCase = true) == true ||
                isSvgContent(bytes)

        if (isLikelySvg) {
            return try {
                ByteArrayInputStream(bytes).use { stream ->
                    val svg = SVG.getFromInputStream(stream)
                    val w = svg.documentWidth.toInt().coerceAtLeast(1)
                    val h = svg.documentHeight.toInt().coerceAtLeast(1)
                    if (w > MAX_IMAGE_DIMENSION || h > MAX_IMAGE_DIMENSION) {
                        ValidationResult.Error("ابعاد تصویر SVG (${w}×${h}) بیش از حد مجاز است (حداکثر $MAX_IMAGE_DIMENSION پیکسل).")
                    } else {
                        ValidationResult.Success("SVG", isSvg = true, width = w, height = h)
                    }
                }
            } catch (e: Exception) {
                ValidationResult.Error("فایل SVG نامعتبر یا آسیب‌دیده است: ${e.localizedMessage ?: "خطای ساختار"}")
            }
        }

        // 2. Check for Bitmap (PNG, JPEG, WebP, GIF) magic headers
        val detectedFormat = detectBitmapFormat(bytes)
        if (detectedFormat == null) {
            return ValidationResult.Error("فرمت فایل تصویری پشتیبانی نمی‌شود. $SUPPORTED_FORMATS_DESC")
        }

        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            val w = options.outWidth
            val h = options.outHeight
            if (w > MAX_IMAGE_DIMENSION || h > MAX_IMAGE_DIMENSION) {
                ValidationResult.Error("ابعاد تصویر (${w}×${h}) بیش از حد مجاز است (حداکثر $MAX_IMAGE_DIMENSION پیکسل).")
            } else {
                ValidationResult.Success(detectedFormat, isSvg = false, width = w, height = h)
            }
        } catch (e: Exception) {
            ValidationResult.Error("فایل تصویری قابل پردازش نیست: ${e.localizedMessage}")
        }
    }

    private fun detectBitmapFormat(bytes: ByteArray): String? {
        if (bytes.size >= 8 &&
            bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
            bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()
        ) {
            return "PNG"
        }
        if (bytes.size >= 3 &&
            bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()
        ) {
            return "JPEG"
        }
        if (bytes.size >= 12 &&
            bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte() &&
            bytes[8] == 'W'.code.toByte() && bytes[9] == 'E'.code.toByte() &&
            bytes[10] == 'B'.code.toByte() && bytes[11] == 'P'.code.toByte()
        ) {
            return "WebP"
        }
        if (bytes.size >= 6 &&
            bytes[0] == 'G'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte()
        ) {
            return "GIF"
        }
        return null
    }

    private fun isSvgContent(bytes: ByteArray): Boolean {
        val sampleLen = minOf(bytes.size, 512)
        val sample = String(bytes, 0, sampleLen, Charsets.UTF_8).trim().lowercase()
        return sample.startsWith("<svg") ||
                sample.contains("<svg") ||
                (sample.startsWith("<?xml") && sample.contains("<svg"))
    }

    private val maxCacheMemory = (Runtime.getRuntime().maxMemory() / 8).toInt().coerceIn(16 * 1024 * 1024, 64 * 1024 * 1024)
    private val memoryCache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(maxCacheMemory) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount
        }
    }

    /**
     * Evicts cached representations of the given file path.
     */
    fun evict(filePath: String) {
        val snapshot = memoryCache.snapshot()
        for (key in snapshot.keys) {
            if (key.startsWith(filePath)) {
                memoryCache.remove(key)
            }
        }
    }

    /**
     * Clears all cached images.
     */
    fun clearCache() {
        memoryCache.evictAll()
    }

    /**
     * Loads either a Bitmap or rasterizes an SVG file into a Bitmap at the requested dimensions.
     * Uses memory cache to prevent repeated decoding during drawing and recomposition.
     */
    fun loadBitmap(file: File, targetSize: Int = 256): Bitmap? {
        if (!file.exists()) return null

        val cacheKey = "${file.absolutePath}:$targetSize:${file.lastModified()}"
        synchronized(memoryCache) {
            val cached = memoryCache.get(cacheKey)
            if (cached != null && !cached.isRecycled) {
                return cached
            }
        }

        val isSvg = file.name.endsWith(".svg", ignoreCase = true)
        val decoded: Bitmap? = if (isSvg) {
            try {
                file.inputStream().use { stream ->
                    renderSvgToBitmap(stream, targetSize, targetSize)
                }
            } catch (e: Exception) {
                null
            }
        } else {
            decodeSampledBitmap(file, targetSize, targetSize)
                ?: try {
                    file.inputStream().use { stream ->
                        renderSvgToBitmap(stream, targetSize, targetSize)
                    }
                } catch (e: Exception) {
                    null
                }
        }

        if (decoded != null) {
            synchronized(memoryCache) {
                memoryCache.put(cacheKey, decoded)
            }
        }

        return decoded
    }

    private fun decodeSampledBitmap(file: File, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)
            if (options.outWidth <= 0 || options.outHeight <= 0) return null

            var inSampleSize = 1
            val (height: Int, width: Int) = options.outHeight to options.outWidth

            if (height > reqHeight || width > reqWidth) {
                val halfHeight: Int = height / 2
                val halfWidth: Int = width / 2
                while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Renders an SVG stream into an Android ARGB_8888 Bitmap preserving aspect ratio.
     */
    fun renderSvgToBitmap(stream: InputStream, width: Int, height: Int): Bitmap? {
        return try {
            val svg = SVG.getFromInputStream(stream)
            val docW = svg.documentWidth
            val docH = svg.documentHeight
            val targetW: Int
            val targetH: Int

            if (docW > 0 && docH > 0) {
                val ratio = docW / docH
                if (ratio >= 1f) {
                    targetW = width
                    targetH = (width / ratio).toInt().coerceAtLeast(1)
                } else {
                    targetH = height
                    targetW = (height * ratio).toInt().coerceAtLeast(1)
                }
            } else {
                targetW = width
                targetH = height
            }

            val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            val rect = RectF(0f, 0f, targetW.toFloat(), targetH.toFloat())
            svg.renderToCanvas(canvas, rect)
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}
