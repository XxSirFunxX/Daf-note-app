package com.example.model

/**
 * 4-Line Staff representation:
 * - Line 1 (bottom): Bass (Tom / تُم)
 * - Line 2 & 3: Middle (Mianeh / میانه)
 * - Line 4 (top): Treble/Border (Bak / بَک) and Ring Sounds (حلقه‌ها)
 */
data class StaffSystem(
    val index: Int,
    val left: Float,
    val right: Float,
    val lineSpacing: Float = 16f,
    val line4SpacingRatio: Float = 1.0f,
    val baseY: Float // Y coordinate of Line 1 (bottom line)
) {
    val line1Y: Float get() = baseY
    val line2Y: Float get() = baseY - lineSpacing
    val line3Y: Float get() = baseY - (2f * lineSpacing)
    val line4Y: Float get() = baseY - (2f * lineSpacing) - (lineSpacing * line4SpacingRatio)

    val topY: Float get() = line4Y - lineSpacing
    val bottomY: Float get() = line1Y + lineSpacing

    fun getLineY(lineNum: Int): Float {
        return when (lineNum) {
            1 -> line1Y
            2 -> line2Y
            3 -> line3Y
            4 -> line4Y
            else -> line1Y
        }
    }

    val snapPositions: List<Float>
        get() = listOf(
            line1Y + (lineSpacing * 0.5f), // Below line 1
            line1Y, // Line 1 (Tom)
            (line1Y + line2Y) / 2f, // Space between line 1 & 2
            line2Y, // Line 2 (Mianeh)
            (line2Y + line3Y) / 2f, // Space between line 2 & 3
            line3Y, // Line 3
            (line3Y + line4Y) / 2f, // Space between line 3 & 4
            line4Y, // Line 4 (Bak / Ring sounds)
            line4Y - (lineSpacing * 0.5f) // Above line 4
        )
}

object StaffSnapper {
    /**
     * Enforces placement rules:
     * - Ring sounds MUST ALWAYS be constrained to Line 4 of the nearest staff!
     * - Normal symbols snap to the nearest line or space if snap mode is active.
     */
    fun calculatePosition(
        candidateX: Float,
        candidateY: Float,
        isRingSound: Boolean,
        isSnapMode: Boolean,
        staves: List<StaffSystem>
    ): Pair<Float, Float> {
        if (staves.isEmpty()) return candidateX to candidateY

        val staff = staves.minByOrNull {
            val centerY = (it.line1Y + it.line4Y) / 2f
            Math.abs(centerY - candidateY)
        } ?: staves.first()

        val clampedX = candidateX.coerceIn(staff.left, staff.right)

        // Strict constraint for Ring sounds (صدای حلقه‌ها)
        if (isRingSound) {
            return clampedX to staff.line4Y
        }

        if (!isSnapMode) {
            return clampedX to candidateY
        }

        val bestY = staff.snapPositions.minByOrNull { Math.abs(it - candidateY) } ?: candidateY
        return clampedX to bestY
    }
}
