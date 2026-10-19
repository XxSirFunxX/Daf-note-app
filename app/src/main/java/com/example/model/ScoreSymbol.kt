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
        val s = scale * (baseSpacing / 14f)
        return when (category) {
            SymbolCategory.BAR_LINES -> (10f * s) to (baseSpacing * 3.8f)
            SymbolCategory.TIME_SIGNATURES -> (22f * s) to (baseSpacing * 2.8f)
            SymbolCategory.NOTES -> (18f * s) to (24f * s)
            SymbolCategory.RESTS -> (16f * s) to (22f * s)
            SymbolCategory.RING_SOUNDS -> (18f * s) to (18f * s)
            SymbolCategory.TECHNIQUES -> (14f * s) to (14f * s)
            SymbolCategory.TEMPO_TEXT -> (36f * s) to (16f * s)
            SymbolCategory.CUSTOM -> (24f * s) to (24f * s)
        }
    }
}
