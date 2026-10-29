package com.example.editor

object ScoreTransformMath {
    const val MIN_ZOOM = 0.2f
    const val MAX_ZOOM = 8.0f

    /**
     * Converts a touch coordinate on screen into exact page logical coordinates.
     */
    fun screenToLogical(
        screenX: Float,
        screenY: Float,
        containerWidthPx: Float,
        containerHeightPx: Float,
        renderedWidthPx: Float,
        renderedHeightPx: Float,
        pageW: Float,
        pageH: Float,
        zoomScale: Float,
        panOffsetX: Float,
        panOffsetY: Float
    ): Pair<Float, Float> {
        val safeZoom = zoomScale.coerceIn(MIN_ZOOM, MAX_ZOOM)
        val dx = screenX - (containerWidthPx / 2f) - panOffsetX
        val dy = screenY - (containerHeightPx / 2f) - panOffsetY
        val paperDx = dx / safeZoom
        val paperDy = dy / safeZoom
        val localX = paperDx + (renderedWidthPx / 2f)
        val localY = paperDy + (renderedHeightPx / 2f)
        val logicalX = (localX / renderedWidthPx) * pageW
        val logicalY = (localY / renderedHeightPx) * pageH
        return logicalX to logicalY
    }

    /**
     * Converts a page logical coordinate back to screen coordinates.
     * Guaranteed to be the exact mathematical inverse of screenToLogical.
     */
    fun logicalToScreen(
        logicalX: Float,
        logicalY: Float,
        containerWidthPx: Float,
        containerHeightPx: Float,
        renderedWidthPx: Float,
        renderedHeightPx: Float,
        pageW: Float,
        pageH: Float,
        zoomScale: Float,
        panOffsetX: Float,
        panOffsetY: Float
    ): Pair<Float, Float> {
        val safeZoom = zoomScale.coerceIn(MIN_ZOOM, MAX_ZOOM)
        val localX = (logicalX / pageW) * renderedWidthPx
        val localY = (logicalY / pageH) * renderedHeightPx
        val paperDx = localX - (renderedWidthPx / 2f)
        val paperDy = localY - (renderedHeightPx / 2f)
        val dx = paperDx * safeZoom
        val dy = paperDy * safeZoom
        val screenX = dx + (containerWidthPx / 2f) + panOffsetX
        val screenY = dy + (containerHeightPx / 2f) + panOffsetY
        return screenX to screenY
    }

    /**
     * Calculates the new pan offset when zooming around an arbitrary focal point (centroid) on screen.
     * Ensures that the logical position directly beneath the focal point does not jump or drift.
     */
    fun calculateZoomAnchor(
        oldZoom: Float,
        newZoom: Float,
        oldPanX: Float,
        oldPanY: Float,
        centroidX: Float,
        centroidY: Float,
        containerWidthPx: Float,
        containerHeightPx: Float,
        panDeltaX: Float = 0f,
        panDeltaY: Float = 0f
    ): Pair<Float, Float> {
        if (oldZoom <= 0f || newZoom <= 0f || !oldZoom.isFinite() || !newZoom.isFinite()) {
            return oldPanX to oldPanY
        }
        val relCenterX = centroidX - (containerWidthPx / 2f)
        val relCenterY = centroidY - (containerHeightPx / 2f)

        val zoomRatio = newZoom / oldZoom
        val newPanX = oldPanX + panDeltaX - (relCenterX - oldPanX) * (zoomRatio - 1f)
        val newPanY = oldPanY + panDeltaY - (relCenterY - oldPanY) * (zoomRatio - 1f)
        return newPanX to newPanY
    }

    /**
     * Generous boundary safety clamp: keeps at least part of the page reachable
     * without imposing arbitrary restrictions.
     */
    fun clampPan(
        panX: Float,
        panY: Float,
        containerWidthPx: Float,
        containerHeightPx: Float,
        renderedWidthPx: Float,
        renderedHeightPx: Float,
        zoomScale: Float
    ): Pair<Float, Float> {
        val safeZoom = zoomScale.coerceIn(MIN_ZOOM, MAX_ZOOM)
        val maxPanX = (containerWidthPx + renderedWidthPx * safeZoom) * 1.5f
        val maxPanY = (containerHeightPx + renderedHeightPx * safeZoom) * 1.5f
        val clampedX = if (panX.isFinite()) panX.coerceIn(-maxPanX, maxPanX) else 0f
        val clampedY = if (panY.isFinite()) panY.coerceIn(-maxPanY, maxPanY) else 0f
        return clampedX to clampedY
    }

    /**
     * Hit-testing for musical symbols on the score page.
     * Provides an adaptive touch target (in logical coordinates) that scales inversely with zoom,
     * allowing reliable finger selection even when zoomed out.
     * When multiple symbols overlap, prioritizes the one closest to the touch point,
     * with a slight preference given to symbols that are already selected.
     */
    fun findHitSymbol(
        logicalX: Float,
        logicalY: Float,
        symbols: List<com.example.model.ScoreSymbol>,
        lineSpacing: Float,
        zoomScale: Float = 1.0f,
        selectedSymbolIds: Set<String> = emptySet()
    ): com.example.model.ScoreSymbol? {
        if (symbols.isEmpty()) return null

        val safeZoom = zoomScale.coerceIn(MIN_ZOOM, MAX_ZOOM)
        // Adaptive touch padding: increases in logical units when zoomed out so on-screen target remains generous
        val extraPadding = (18f / safeZoom.coerceAtLeast(0.5f)).coerceIn(16f, 36f)

        val candidates = symbols.filter { sym ->
            val (hw, hh) = sym.getVisualBounds(lineSpacing)
            val touchHw = maxOf(hw + extraPadding, 24f)
            val touchHh = maxOf(hh + extraPadding, 24f)

            if (sym.rotation != 0f) {
                val dx = (logicalX - sym.x).toDouble()
                val dy = (logicalY - sym.y).toDouble()
                val rad = Math.toRadians(-sym.rotation.toDouble())
                val cos = Math.cos(rad)
                val sin = Math.sin(rad)
                val localX = (dx * cos - dy * sin).toFloat()
                val localY = (dx * sin + dy * cos).toFloat()
                localX in -touchHw..touchHw && localY in -touchHh..touchHh
            } else {
                logicalX in (sym.x - touchHw)..(sym.x + touchHw) &&
                logicalY in (sym.y - touchHh)..(sym.y + touchHh)
            }
        }

        if (candidates.isEmpty()) return null

        // Pick the closest symbol to the touch point (Euclidean distance)
        // Selected symbols get a 25% distance advantage to avoid accidental switching
        return candidates.minByOrNull { sym ->
            val dx = logicalX - sym.x
            val dy = logicalY - sym.y
            val distSq = dx * dx + dy * dy
            if (selectedSymbolIds.contains(sym.id)) distSq * 0.75f else distSq
        }
    }
}
