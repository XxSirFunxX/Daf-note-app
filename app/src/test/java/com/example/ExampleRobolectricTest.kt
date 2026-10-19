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
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun readAppNameFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("نت‌نگار چهارخطی", appName)
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
}
