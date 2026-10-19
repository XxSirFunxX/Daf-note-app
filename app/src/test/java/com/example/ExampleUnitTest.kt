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
}
