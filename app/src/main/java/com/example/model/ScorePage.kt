package com.example.model

import java.util.UUID

data class ScorePage(
    val id: String = UUID.randomUUID().toString(),
    val pageNumber: Int = 1,
    val title: String = "درس دو ضربی ساده",
    val subtitle: String = "تمپو: Andante ♩=80",
    val lessonNumber: String = "۱",
    val paperSize: PaperSize = PaperSize.A4,
    val stavesCount: Int = 5,
    val lineSpacing: Float = 16f,
    val line4SpacingRatio: Float = 1.0f,
    val marginTop: Float = 110f,
    val marginBottom: Float = 80f,
    val marginLeft: Float = 60f,
    val marginRight: Float = 60f,
    val symbols: List<ScoreSymbol> = emptyList()
) {
    /**
     * Dynamically generates the 4-line staff systems for this page layout.
     */
    fun generateStaves(): List<StaffSystem> {
        val staves = mutableListOf<StaffSystem>()
        if (stavesCount <= 0) return staves

        val availableWidth = paperSize.widthPt - marginLeft - marginRight
        val staffLeft = marginLeft
        val staffRight = marginLeft + availableWidth

        val staffHeight = (2f * lineSpacing) + (lineSpacing * line4SpacingRatio)
        val availableHeight = paperSize.heightPt - marginTop - marginBottom
        val totalStaffOccupiedHeight = stavesCount * staffHeight
        val remainingSpace = (availableHeight - totalStaffOccupiedHeight).coerceAtLeast(0f)
        val gap = if (stavesCount > 1) remainingSpace / (stavesCount - 1) else 60f

        for (i in 0 until stavesCount) {
            val topOfThisStaff = marginTop + (i * (staffHeight + gap))
            val line1BaseY = topOfThisStaff + staffHeight // Line 1 is the bottom line
            staves.add(
                StaffSystem(
                    index = i,
                    left = staffLeft,
                    right = staffRight,
                    lineSpacing = lineSpacing,
                    line4SpacingRatio = line4SpacingRatio,
                    baseY = line1BaseY
                )
            )
        }

        return staves
    }
}
