package com.example.model

import java.util.UUID

data class ScoreSymbol(
    val id: String = UUID.randomUUID().toString(),
    val type: SymbolType,
    val category: SymbolCategory = type.defaultCategory,
    val name: String = type.defaultTitleFa,
    val x: Float, // Logical X coordinate
    val y: Float, // Logical Y coordinate
    val scale: Float = type.defaultScale,
    val rotation: Float = 0f,
    val isStemUp: Boolean = true,
    val isRingSound: Boolean = (category == SymbolCategory.RING_SOUNDS || type.isRingSound),
    val textValue: String? = null,
    val customData: String? = null,
    val colorHex: String = "#1E1C1A",
    val imagePath: String? = null,
    val durationValue: String? = null
) {
    /**
     * Approximate bounding box half-width and half-height for hit-testing and selection box.
     */
    fun getVisualBounds(baseSpacing: Float): Pair<Float, Float> {
        val s = scale * (baseSpacing / 16f)
        if (type == SymbolType.TEXT_ANNOTATION) {
            val text = textValue ?: name
            val lines = text.split("\n")
            val maxLen = lines.maxOfOrNull { it.length } ?: 4
            val hw = ((maxLen * 4.5f + 10f) * s).coerceIn(20f * s, 300f * s)
            val hh = ((lines.size * 11f + 6f) * s).coerceIn(12f * s, 200f * s)
            return hw to hh
        }
        return when (category) {
            SymbolCategory.BAR_LINES -> (10f * s) to (baseSpacing * 2.0f * s)
            SymbolCategory.TIME_SIGNATURES -> (16f * s) to (baseSpacing * 1.5f * s)
            SymbolCategory.NOTES -> (16f * s) to (22f * s)
            SymbolCategory.RESTS -> (14f * s) to (20f * s)
            SymbolCategory.RING_SOUNDS -> (16f * s) to (16f * s)
            SymbolCategory.TECHNIQUES -> (14f * s) to (14f * s)
            SymbolCategory.TEMPO_TEXT -> (32f * s) to (16f * s)
            SymbolCategory.CUSTOM -> (16f * s) to (16f * s)
        }
    }
}
