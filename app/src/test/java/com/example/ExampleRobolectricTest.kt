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

    @Test
    fun testCustomSymbolSerialization_preservesAllMetadata() {
        val customSym = ScoreSymbol(
            type = SymbolType.CUSTOM_SYMBOL,
            category = com.example.model.SymbolCategory.CUSTOM,
            name = "حلقه ترکیبی ویژه",
            x = 220f,
            y = 80f,
            isRingSound = true,
            imagePath = "/data/data/com.aistudio.chaharkhat.notnegar/files/symbols/sym_1.png",
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
        assertEquals("/data/data/com.aistudio.chaharkhat.notnegar/files/symbols/sym_1.png", importedSym?.imagePath)
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
}
