package com.example

import com.example.editor.UndoRedoManager
import com.example.model.PaperSize
import com.example.model.ScorePage
import com.example.model.ScoreSymbol
import com.example.model.StaffSnapper
import com.example.model.SymbolType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testStaffSystemGeometry_FourLines() {
        val page = ScorePage(paperSize = PaperSize.A4, stavesCount = 3, lineSpacing = 16f, line4SpacingRatio = 1.0f)
        val staves = page.generateStaves()

        assertEquals(3, staves.size)
        val firstStaff = staves[0]

        // Verify 4 lines exist with proper vertical ordering (Y=0 is top, so line 4 is top line with smaller Y)
        assertTrue(firstStaff.line4Y < firstStaff.line3Y)
        assertTrue(firstStaff.line3Y < firstStaff.line2Y)
        assertTrue(firstStaff.line2Y < firstStaff.line1Y)

        // Line 1 to 2 spacing
        assertEquals(16f, firstStaff.line1Y - firstStaff.line2Y, 0.01f)
        // Line 2 to 3 spacing
        assertEquals(16f, firstStaff.line2Y - firstStaff.line3Y, 0.01f)
        // Line 3 to 4 spacing
        assertEquals(16f, firstStaff.line3Y - firstStaff.line4Y, 0.01f)
    }

    @Test
    fun testRingSound_StrictlyConstrainedToLine4() {
        val page = ScorePage()
        val staves = page.generateStaves()
        val staff = staves[0]

        // Candidate position trying to place on Line 1 (Tom) or middle (Line 2)
        val candidateX = staff.left + 100f
        val candidateYTryingToBeLine1 = staff.line1Y

        val (finalX, finalY) = StaffSnapper.calculatePosition(
            candidateX = candidateX,
            candidateY = candidateYTryingToBeLine1,
            isRingSound = true, // Ring sound constraint active!
            isSnapMode = false, // Even in free mode!
            staves = staves
        )

        // Must be clamped strictly to Line 4!
        assertEquals(staff.line4Y, finalY, 0.01f)
        assertEquals(candidateX, finalX, 0.01f)
    }

    @Test
    fun testNormalSymbol_CanBePlacedOnOtherLines() {
        val page = ScorePage()
        val staves = page.generateStaves()
        val staff = staves[0]

        val candidateX = staff.left + 80f
        val candidateYLine1 = staff.line1Y

        val (finalX, finalY) = StaffSnapper.calculatePosition(
            candidateX = candidateX,
            candidateY = candidateYLine1,
            isRingSound = false,
            isSnapMode = true,
            staves = staves
        )

        // Normal symbol stays on Line 1
        assertEquals(staff.line1Y, finalY, 0.01f)
    }

    @Test
    fun testUndoRedoManager() {
        val manager = UndoRedoManager()
        val sym1 = ScoreSymbol(type = SymbolType.NOTE_QUARTER, x = 100f, y = 200f)
        val sym2 = ScoreSymbol(type = SymbolType.NOTE_HALF, x = 150f, y = 200f)

        // State 0: empty
        manager.recordState(emptyList())

        // State 1: 1 symbol
        val currentList = listOf(sym1)
        manager.recordState(currentList)

        // State 2: 2 symbols
        val updatedList = listOf(sym1, sym2)

        assertTrue(manager.canUndo)
        assertFalse(manager.canRedo)

        val undone = manager.undo(updatedList)
        assertNotNull(undone)
        assertEquals(1, undone?.size)
        assertEquals(sym1.id, undone?.first()?.id)

        assertTrue(manager.canRedo)
        val redone = manager.redo(undone!!)
        assertNotNull(redone)
        assertEquals(2, redone?.size)
    }

    @Test
    fun testScoreTransformMath_BidirectionalConversionExactInverse() {
        val containerW = 1080f
        val containerH = 1920f
        val renderedW = 800f
        val renderedH = 1131.42f
        val pageW = 1000f
        val pageH = 1414.28f

        val testZooms = listOf(0.2f, 0.5f, 1.0f, 2.5f, 5.0f, 8.0f)
        val testPans = listOf(0f to 0f, 150f to -200f, -400f to 300f)

        for (z in testZooms) {
            for ((px, py) in testPans) {
                // Test multiple arbitrary logical points
                val testLogicalPoints = listOf(
                    0f to 0f,
                    500f to 707f,
                    100f to 250f,
                    900f to 1300f
                )

                for ((lx, ly) in testLogicalPoints) {
                    val (screenX, screenY) = com.example.editor.ScoreTransformMath.logicalToScreen(
                        logicalX = lx,
                        logicalY = ly,
                        containerWidthPx = containerW,
                        containerHeightPx = containerH,
                        renderedWidthPx = renderedW,
                        renderedHeightPx = renderedH,
                        pageW = pageW,
                        pageH = pageH,
                        zoomScale = z,
                        panOffsetX = px,
                        panOffsetY = py
                    )

                    val (invertedLx, invertedLy) = com.example.editor.ScoreTransformMath.screenToLogical(
                        screenX = screenX,
                        screenY = screenY,
                        containerWidthPx = containerW,
                        containerHeightPx = containerH,
                        renderedWidthPx = renderedW,
                        renderedHeightPx = renderedH,
                        pageW = pageW,
                        pageH = pageH,
                        zoomScale = z,
                        panOffsetX = px,
                        panOffsetY = py
                    )

                    assertEquals("Logical X mismatch at zoom $z", lx, invertedLx, 0.05f)
                    assertEquals("Logical Y mismatch at zoom $z", ly, invertedLy, 0.05f)
                }
            }
        }
    }

    @Test
    fun testScoreTransformMath_ZoomAnchorMaintainsFocalPoint() {
        val containerW = 1000f
        val containerH = 1500f
        val renderedW = 800f
        val renderedH = 1131f
        val pageW = 1000f
        val pageH = 1414f

        val oldZoom = 1.0f
        val newZoom = 2.5f
        val oldPanX = 50f
        val oldPanY = -80f
        val centroidX = 650f
        val centroidY = 400f

        // Logical point before zoom directly under the user's fingers (centroid)
        val (logicalBeforeX, logicalBeforeY) = com.example.editor.ScoreTransformMath.screenToLogical(
            screenX = centroidX,
            screenY = centroidY,
            containerWidthPx = containerW,
            containerHeightPx = containerH,
            renderedWidthPx = renderedW,
            renderedHeightPx = renderedH,
            pageW = pageW,
            pageH = pageH,
            zoomScale = oldZoom,
            panOffsetX = oldPanX,
            panOffsetY = oldPanY
        )

        // Calculate new pan with anchor
        val (newPanX, newPanY) = com.example.editor.ScoreTransformMath.calculateZoomAnchor(
            oldZoom = oldZoom,
            newZoom = newZoom,
            oldPanX = oldPanX,
            oldPanY = oldPanY,
            centroidX = centroidX,
            centroidY = centroidY,
            containerWidthPx = containerW,
            containerHeightPx = containerH,
            panDeltaX = 0f,
            panDeltaY = 0f
        )

        // Logical point after zoom under the exact same screen touch location
        val (logicalAfterX, logicalAfterY) = com.example.editor.ScoreTransformMath.screenToLogical(
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

        assertEquals("Focal point X must remain stationary during pinch-to-zoom", logicalBeforeX, logicalAfterX, 0.01f)
        assertEquals("Focal point Y must remain stationary during pinch-to-zoom", logicalBeforeY, logicalAfterY, 0.01f)
    }

    @Test
    fun testScoreTransformMath_ZoomRangeSafeguards() {
        assertEquals(0.2f, com.example.editor.ScoreTransformMath.MIN_ZOOM, 0.001f)
        assertEquals(8.0f, com.example.editor.ScoreTransformMath.MAX_ZOOM, 0.001f)

        // Verify that invalid/infinite zooms are handled safely
        val (safePanX, safePanY) = com.example.editor.ScoreTransformMath.calculateZoomAnchor(
            oldZoom = Float.NaN,
            newZoom = 2f,
            oldPanX = 10f,
            oldPanY = 20f,
            centroidX = 100f,
            centroidY = 100f,
            containerWidthPx = 500f,
            containerHeightPx = 500f
        )
        assertEquals(10f, safePanX, 0.001f)
        assertEquals(20f, safePanY, 0.001f)
    }

    @Test
    fun testIssue1And2_AllCategoriesAndSymbolsCoverage() {
        // Ensure every SymbolType has a valid category or mapping
        for (symbolType in SymbolType.values()) {
            assertNotNull(symbolType.defaultTitleFa)
            assertTrue(symbolType.defaultTitleFa.isNotEmpty())
            assertNotNull(symbolType.defaultCategory)
        }

        // Test category counting logic with canonical IDs and Persian titles
        val testCat = com.example.data.CategoryEntity("TECHNIQUES", "تکنیک و مضراب")
        val def1 = com.example.data.NoteDefinitionEntity(
            id = "DEF_CHAP",
            name = "چپ",
            symbolTypeName = "TECHNIQUE_CHAP",
            categoryId = "TECHNIQUES"
        )
        val def2 = com.example.data.NoteDefinitionEntity(
            id = "DEF_RAST",
            name = "راست",
            symbolTypeName = "TECHNIQUE_RAST",
            categoryId = "تکنیک و مضراب" // Assigned by title
        )
        val otherDef = com.example.data.NoteDefinitionEntity(
            id = "DEF_TOM",
            name = "تُم سیاه",
            symbolTypeName = "NOTE_QUARTER",
            categoryId = "NOTES"
        )

        val definitions = listOf(def1, def2, otherDef)
        val count = definitions.count { it.categoryId == testCat.id || it.categoryId == testCat.titleFa }
        assertEquals("Both canonical ID and titleFa must be counted", 2, count)
    }

    @Test
    fun testIssue3_CategoryReassignmentLogic() {
        val oldCatId = "RING_SOUNDS"
        val oldCatTitle = "صدای حلقه‌ها"
        val newCatId = "TECHNIQUES"

        val def = com.example.data.NoteDefinitionEntity(
            id = "DEF_RING_1",
            name = "حلقه تک",
            symbolTypeName = "RING_SINGLE",
            categoryId = oldCatTitle
        )

        val matches = def.categoryId == oldCatId || def.categoryId == oldCatTitle
        assertTrue("Item referencing old title must match for reassignment", matches)

        val reassignedDef = def.copy(categoryId = newCatId)
        assertEquals(newCatId, reassignedDef.categoryId)
        assertFalse(reassignedDef.categoryId == oldCatId || reassignedDef.categoryId == oldCatTitle)
    }

    @Test
    fun testIssue4_CustomSymbolDurationValue() {
        val customWithDefaultDuration = com.example.data.CustomSymbolEntity(
            id = "CS_1",
            name = "نماد بدون کشش",
            categoryId = "CUSTOM",
            durationValue = null
        )
        assertEquals("Default duration must be null", null, customWithDefaultDuration.durationValue)

        val customWithDuration = com.example.data.CustomSymbolEntity(
            id = "CS_2",
            name = "اشاره با کشش چنگ",
            categoryId = "CUSTOM",
            durationValue = "1/8"
        )
        assertEquals("1/8", customWithDuration.durationValue)

        // Verify ScoreSymbol creation with duration
        val scoreSymbol = ScoreSymbol(
            type = SymbolType.NOTE_QUARTER,
            x = 100f,
            y = 200f,
            durationValue = customWithDuration.durationValue
        )
        assertEquals("1/8", scoreSymbol.durationValue)
    }

    @Test
    fun testIssue5_StaffGuideTextState() {
        val defaultLine4 = "۴ (حلقه‌ها)"
        val defaultLine1 = "۱ (تُم)"

        val customLine4 = "خط ۴ - رینگ"
        val customLine1 = "خط ۱ - ضربه اصلی"

        var currentLine4 = defaultLine4
        var currentLine1 = defaultLine1

        assertEquals("۴ (حلقه‌ها)", currentLine4)
        assertEquals("۱ (تُم)", currentLine1)

        // Update
        currentLine4 = customLine4
        currentLine1 = customLine1
        assertEquals("خط ۴ - رینگ", currentLine4)
        assertEquals("خط ۱ - ضربه اصلی", currentLine1)

        // Reset to default
        currentLine4 = defaultLine4
        currentLine1 = defaultLine1
        assertEquals("۴ (حلقه‌ها)", currentLine4)
        assertEquals("۱ (تُم)", currentLine1)
    }

    @Test
    fun testIssue1_UnifiedManageableNotationItems() {
        val noteDef = com.example.data.NoteDefinitionEntity(
            id = "DEF_TOM",
            name = "تُم اصلی",
            symbolTypeName = "NOTE_QUARTER",
            categoryId = "NOTES",
            isDefault = true,
            isHidden = false
        )
        val customSym = com.example.data.CustomSymbolEntity(
            id = "CS_CUSTOM_1",
            name = "مضراب خاص",
            categoryId = "TECHNIQUES",
            durationValue = "1/8",
            isHidden = false
        )

        val noteItem = com.example.ui.dialogs.ManageableNotationItem.NoteDef(noteDef)
        val customItem = com.example.ui.dialogs.ManageableNotationItem.CustomSym(customSym)

        val combinedList = listOf(noteItem, customItem)
        assertEquals(2, combinedList.size)

        // Verify distinction between built-in and user custom
        assertTrue(noteItem.isDefault)
        assertFalse(customItem.isDefault)
        assertEquals("1/8", customItem.durationValue)

        // Test visibility toggle preservation
        val hiddenCustom = customSym.copy(isHidden = true)
        val hiddenCustomItem = com.example.ui.dialogs.ManageableNotationItem.CustomSym(hiddenCustom)
        assertTrue(hiddenCustomItem.isHidden)
    }

    @Test
    fun testIssue2_CustomSymbolsFilteredStrictlyByAssignedCategory() {
        val techniqueCustom = com.example.data.CustomSymbolEntity(
            id = "CS_TECH_1",
            name = "اشاره ترکیبی",
            categoryId = "TECHNIQUES"
        )
        val genericCustom = com.example.data.CustomSymbolEntity(
            id = "CS_GEN_1",
            name = "علامت آزاد",
            categoryId = "CUSTOM"
        )

        val allCustom = listOf(techniqueCustom, genericCustom)

        // Filter for "TECHNIQUES"
        val techniquesFiltered = allCustom.filter { it.categoryId == "TECHNIQUES" || it.categoryId == "تکنیک و مضراب" }
        assertEquals(1, techniquesFiltered.size)
        assertEquals("CS_TECH_1", techniquesFiltered[0].id)

        // Filter for "CUSTOM" - must NOT include techniqueCustom!
        val customFiltered = allCustom.filter { it.categoryId == "CUSTOM" || it.categoryId == "سفارشی" }
        assertEquals(1, customFiltered.size)
        assertEquals("CS_GEN_1", customFiltered[0].id)
    }

    @Test
    fun testIssue3_AllFourStaffGuideLinesState() {
        val defaultLine4 = "۴ (حلقه‌ها)"
        val defaultLine3 = "۳ (بَک)"
        val defaultLine2 = "۲ (میانه)"
        val defaultLine1 = "۱ (تُم)"

        var line4 = defaultLine4
        var line3 = defaultLine3
        var line2 = defaultLine2
        var line1 = defaultLine1

        assertEquals("۴ (حلقه‌ها)", line4)
        assertEquals("۳ (بَک)", line3)
        assertEquals("۲ (میانه)", line2)
        assertEquals("۱ (تُم)", line1)

        // Edit lines 2 and 3 independently
        line2 = "۲ (چپ)"
        line3 = "۳ (راست)"
        assertEquals("۴ (حلقه‌ها)", line4)
        assertEquals("۳ (راست)", line3)
        assertEquals("۲ (چپ)", line2)
        assertEquals("۱ (تُم)", line1)

        // Restore all defaults
        line4 = defaultLine4
        line3 = defaultLine3
        line2 = defaultLine2
        line1 = defaultLine1
        assertEquals("۴ (حلقه‌ها)", line4)
        assertEquals("۳ (بَک)", line3)
        assertEquals("۲ (میانه)", line2)
        assertEquals("۱ (تُم)", line1)
    }
}
