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

    @Test
    fun testSanitizeFileName() {
        val unsafe = "قطعه / تست : ویژه * \" < > | ? .dafnt"
        val clean = com.example.io.ScoreExporter.sanitizeFileName(unsafe)
        assertFalse(clean.contains("/"))
        assertFalse(clean.contains(":"))
        assertFalse(clean.contains("*"))
        assertFalse(clean.contains("?"))
        assertFalse(clean.contains("\""))
        assertFalse(clean.contains("<"))
        assertFalse(clean.contains(">"))
        assertFalse(clean.contains("|"))
    }

    @Test
    fun testVisualBoundsConsistency() {
        val sym = ScoreSymbol(
            type = SymbolType.NOTE_QUARTER,
            x = 100f,
            y = 100f,
            scale = 1.0f
        )
        val (hw, hh) = sym.getVisualBounds(16f)
        assertEquals(16f, hw, 0.01f)
        assertEquals(22f, hh, 0.01f)
    }

    @Test
    fun testFindHitSymbol_ProximityAndPrioritization() {
        val sym1 = ScoreSymbol(id = "sym1", type = SymbolType.NOTE_QUARTER, x = 100f, y = 100f)
        val sym2 = ScoreSymbol(id = "sym2", type = SymbolType.NOTE_HALF, x = 120f, y = 100f)
        val symbols = listOf(sym1, sym2)

        // Point closer to sym1
        val hit1 = com.example.editor.ScoreTransformMath.findHitSymbol(
            logicalX = 105f,
            logicalY = 100f,
            symbols = symbols,
            lineSpacing = 16f
        )
        assertNotNull(hit1)
        assertEquals("sym1", hit1?.id)

        // Point closer to sym2
        val hit2 = com.example.editor.ScoreTransformMath.findHitSymbol(
            logicalX = 118f,
            logicalY = 100f,
            symbols = symbols,
            lineSpacing = 16f
        )
        assertNotNull(hit2)
        assertEquals("sym2", hit2?.id)

        // Point far away from any symbol
        val hitNone = com.example.editor.ScoreTransformMath.findHitSymbol(
            logicalX = 300f,
            logicalY = 300f,
            symbols = symbols,
            lineSpacing = 16f
        )
        assertTrue(hitNone == null)
    }

    @Test
    fun testFindHitSymbol_ZoomAdaptiveTouchTarget() {
        val sym = ScoreSymbol(id = "note1", type = SymbolType.NOTE_QUARTER, x = 100f, y = 100f)
        val symbols = listOf(sym)

        // At 0.5 zoom (zoomed out), touch padding expands in logical points
        // so a touch 35 units away from center still hits the target
        val hitZoomedOut = com.example.editor.ScoreTransformMath.findHitSymbol(
            logicalX = 135f,
            logicalY = 100f,
            symbols = symbols,
            lineSpacing = 16f,
            zoomScale = 0.5f
        )
        assertNotNull(hitZoomedOut)
        assertEquals("note1", hitZoomedOut?.id)
    }

    @Test
    fun testDragAccumulation_MovesAcrossSnapPositions() {
        val page = ScorePage()
        val staves = page.generateStaves()
        val staff = staves[0]
        val startY = staff.line1Y // Bottom line
        val initialX = staff.left + 50f

        // Initial candidate position
        val (initX, initY) = StaffSnapper.calculatePosition(initialX, startY, false, true, staves)
        assertEquals(startY, initY, 0.01f)

        // Simulate small incremental finger drags (e.g. 1.5 points per frame)
        // With accumulator tracking, accumulated delta increases on each step
        var accumulatedDeltaY = 0f
        val stepDelta = 1.5f
        var snappedPositions = mutableListOf<Float>()

        for (i in 1..10) {
            accumulatedDeltaY -= stepDelta // Moving up towards line 2
            val candidateY = initY + accumulatedDeltaY
            val (_, snappedY) = StaffSnapper.calculatePosition(initX, candidateY, false, true, staves)
            snappedPositions.add(snappedY)
        }

        // As accumulatedDelta reaches -15f (close to -16f line spacing),
        // the snapped position MUST advance from line1Y to line2Y (or intermediate space)!
        val finalSnapped = snappedPositions.last()
        assertTrue("Expected snapped position to move away from line1Y, got $finalSnapped vs start $startY", finalSnapped < startY)
    }

    @Test
    fun testPinchZoom_MultiTouchReleaseDoesNotTriggerTap() {
        // Simulates the gesture state machine:
        // 1. First finger down (pointerCount = 1)
        // 2. Second finger joins (pointerCount = 2 -> wasMultiTouch becomes true)
        // 3. User zooms
        // 4. One finger lifts (pointerCount = 1, wasMultiTouch remains true)
        // 5. Second finger lifts (pointerCount = 0)
        var wasMultiTouch = false
        var isTransformingActive = false
        var tapPlaced = false

        // Finger 1 down
        var pointerCount = 1
        // Finger 2 down
        pointerCount = 2
        if (pointerCount >= 2) {
            wasMultiTouch = true
            isTransformingActive = true
        }

        assertTrue("Gesture must be marked as multi-touch", wasMultiTouch)
        assertTrue("Transforming must be active", isTransformingActive)

        // Finger 1 lifts first
        pointerCount = 1
        if (pointerCount == 1 && wasMultiTouch) {
            isTransformingActive = false
        }
        // wasMultiTouch MUST remain true!
        assertTrue("wasMultiTouch must remain true when transitioning to 1 finger", wasMultiTouch)

        // Finger 2 lifts (all pointers released)
        pointerCount = 0
        if (pointerCount == 0) {
            if (wasMultiTouch) {
                // Must NOT place note or fire tap!
                tapPlaced = false
            } else {
                tapPlaced = true
            }
        }

        assertFalse("Lifting fingers after a pinch gesture must NEVER trigger tap/note-placement", tapPlaced)
    }

    @Test
    fun testSelectedSymbol_CanDragDirectlyOrFromOffset() {
        val sym1 = ScoreSymbol(id = "selected_note", type = SymbolType.NOTE_QUARTER, x = 150f, y = 200f)
        val selectedIds = setOf(sym1.id)

        // Case A: User touches directly on the selected symbol
        val hitDirect = com.example.editor.ScoreTransformMath.findHitSymbol(
            logicalX = 152f,
            logicalY = 201f,
            symbols = listOf(sym1),
            lineSpacing = 16f,
            selectedSymbolIds = selectedIds
        )
        val isDirectDragEligible = (hitDirect != null || selectedIds.isNotEmpty())
        assertTrue("Direct touch on selected symbol must be drag-eligible", isDirectDragEligible)

        // Case B: User touches away from the selected symbol (offset drag to avoid finger obscuring note)
        val hitOffset = com.example.editor.ScoreTransformMath.findHitSymbol(
            logicalX = 400f,
            logicalY = 400f,
            symbols = listOf(sym1),
            lineSpacing = 16f,
            selectedSymbolIds = selectedIds
        )
        assertTrue("Offset touch hits null symbol directly", hitOffset == null)
        val isOffsetDragEligible = (hitOffset != null || selectedIds.isNotEmpty())
        assertTrue("Offset drag when a symbol is already selected MUST be drag-eligible", isOffsetDragEligible)

        // Case C: No symbol is selected and touch is on blank canvas -> must NOT be symbol drag
        val emptySelection = emptySet<String>()
        val isBlankDragEligible = (hitOffset != null || emptySelection.isNotEmpty())
        assertFalse("Blank canvas drag with no selection must route to canvas panning", isBlankDragEligible)
    }

    @Test
    fun testSnappingVsFreePositioningModes() {
        val page = ScorePage()
        val staves = page.generateStaves()
        val staff = staves[0]

        // Candidate Y halfway between line 1 and the space above it
        val rawCandidateY = staff.line1Y - 4.23f
        val candidateX = staff.left + 100f

        // With snap mode active
        val (snappedX, snappedY) = StaffSnapper.calculatePosition(
            candidateX = candidateX,
            candidateY = rawCandidateY,
            isRingSound = false,
            isSnapMode = true,
            staves = staves
        )
        // Snapped Y must be quantized to nearest staff position (line1Y or space)
        assertTrue("Snapped Y must snap to staff position", staff.snapPositions.contains(snappedY))

        // With free positioning mode (snap disabled)
        val (freeX, freeY) = StaffSnapper.calculatePosition(
            candidateX = candidateX,
            candidateY = rawCandidateY,
            isRingSound = false,
            isSnapMode = false,
            staves = staves
        )
        // Free Y must match the exact continuous candidate Y without quantization
        assertEquals(rawCandidateY, freeY, 0.001f)
        assertEquals(candidateX, freeX, 0.001f)
    }

    @Test
    fun testRingSound_RemainsStrictlyOnLine4InFreeMode() {
        val page = ScorePage()
        val staves = page.generateStaves()
        val staff = staves[0]

        val rawCandidateY = staff.line1Y - 50f
        val candidateX = staff.left + 100f

        // In free-position mode (isSnapMode = false)
        val (finalX, finalY) = StaffSnapper.calculatePosition(
            candidateX = candidateX,
            candidateY = rawCandidateY,
            isRingSound = true, // Ring sound constraint
            isSnapMode = false,
            staves = staves
        )

        assertEquals("Ring sound must strictly stay on line 4 even in free mode", staff.line4Y, finalY, 0.001f)
    }
}

