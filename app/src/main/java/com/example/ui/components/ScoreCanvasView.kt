package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.ScorePage
import com.example.model.SymbolType
import com.example.render.ScoreRenderer

@Composable
fun ScoreCanvasView(
    page: ScorePage,
    selectedSymbolIds: Set<String>,
    activePaletteSymbol: SymbolType?,
    activeCustomSymbol: com.example.data.CustomSymbolEntity? = null,
    isSnapMode: Boolean,
    showGuides: Boolean,
    highlightLine4: Boolean,
    zoomScale: Float,
    panOffsetX: Float,
    panOffsetY: Float,
    onZoomAndPanChange: (Float, Float, Float) -> Unit,
    onCanvasTap: (Float, Float) -> Unit,
    onSymbolDrag: (Float, Float) -> Unit,
    onSymbolDragFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFE8E5DF))
            .testTag("score_canvas_container"),
        contentAlignment = Alignment.Center
    ) {
        val containerWidthPx = constraints.maxWidth.toFloat()
        val containerHeightPx = constraints.maxHeight.toFloat()

        // Page logical dimensions
        val pageW = page.paperSize.widthPt
        val pageH = page.paperSize.heightPt

        // Fit scale inside container
        val horizontalScale = (containerWidthPx - 32f) / pageW
        val verticalScale = (containerHeightPx - 32f) / pageH
        val fitScale = minOf(horizontalScale, verticalScale).coerceAtLeast(0.1f)

        val renderedWidthDp = with(density) { (pageW * fitScale).toDp() }
        val renderedHeightDp = with(density) { (pageH * fitScale).toDp() }

        val renderedWidthPx = pageW * fitScale
        val renderedHeightPx = pageH * fitScale

        // Helper to convert screen pointer touch into exact page logical coordinates
        fun screenToLogical(screenX: Float, screenY: Float): Pair<Float, Float> {
            val dx = screenX - (containerWidthPx / 2f) - panOffsetX
            val dy = screenY - (containerHeightPx / 2f) - panOffsetY
            val paperDx = dx / zoomScale
            val paperDy = dy / zoomScale
            val localX = paperDx + (renderedWidthPx / 2f)
            val localY = paperDy + (renderedHeightPx / 2f)
            val logicalX = (localX / renderedWidthPx) * pageW
            val logicalY = (localY / renderedHeightPx) * pageH
            return logicalX to logicalY
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(page.id, selectedSymbolIds, activePaletteSymbol, activeCustomSymbol, zoomScale, panOffsetX, panOffsetY) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val downPos = down.position
                        val (downLogicalX, downLogicalY) = screenToLogical(downPos.x, downPos.y)

                        // Check if down was on a selected symbol or any symbol on page
                        val hitSymbol = page.symbols.lastOrNull { sym ->
                            val (hw, hh) = sym.getVisualBounds(page.lineSpacing)
                            downLogicalX in (sym.x - hw - 14f)..(sym.x + hw + 14f) &&
                            downLogicalY in (sym.y - hh - 14f)..(sym.y + hh + 14f)
                        }

                        var isTransforming = false
                        var isDragging = false
                        val isSymbolDrag = (hitSymbol != null || selectedSymbolIds.isNotEmpty()) &&
                                           activePaletteSymbol == null && activeCustomSymbol == null

                        var prevCentroid = downPos
                        var prevDistance = 0f
                        var currentZoom = zoomScale
                        var currentPanX = panOffsetX
                        var currentPanY = panOffsetY
                        var totalDragDistance = 0f

                        while (true) {
                            val event = awaitPointerEvent()
                            val activePointers = event.changes.filter { it.pressed }

                            if (activePointers.isEmpty()) {
                                // All pointers released
                                if (!isDragging && !isTransforming && totalDragDistance < 15f) {
                                    // Clean tap!
                                    onCanvasTap(downLogicalX, downLogicalY)
                                } else if (isDragging && isSymbolDrag) {
                                    onSymbolDragFinished()
                                }
                                break
                            }

                            if (activePointers.size >= 2) {
                                // Two fingers: Pinch to zoom and 2-finger pan
                                isTransforming = true
                                isDragging = false
                                val p1 = activePointers[0].position
                                val p2 = activePointers[1].position
                                val currentDistance = (p1 - p2).getDistance()
                                val currentCentroid = (p1 + p2) / 2f

                                if (prevDistance > 0f && currentDistance > 0f) {
                                    val zoomFactor = currentDistance / prevDistance
                                    val newZoom = (currentZoom * zoomFactor).coerceIn(0.5f, 4.0f)
                                    val factorChange = newZoom / currentZoom

                                    val panDelta = currentCentroid - prevCentroid
                                    currentPanX = (currentPanX + panDelta.x) * factorChange + (1f - factorChange) * (currentCentroid.x - containerWidthPx / 2f)
                                    currentPanY = (currentPanY + panDelta.y) * factorChange + (1f - factorChange) * (currentCentroid.y - containerHeightPx / 2f)
                                    currentZoom = newZoom

                                    onZoomAndPanChange(currentZoom, currentPanX, currentPanY)
                                }

                                prevDistance = currentDistance
                                prevCentroid = currentCentroid
                                event.changes.forEach { it.consume() }
                            } else if (activePointers.size == 1 && !isTransforming) {
                                // Single finger: drag symbol or pan canvas
                                val change = activePointers[0]
                                val dragDelta = change.position - change.previousPosition
                                totalDragDistance += dragDelta.getDistance()

                                if (totalDragDistance > 10f) {
                                    isDragging = true
                                    change.consume()

                                    if (isSymbolDrag && activePaletteSymbol == null && activeCustomSymbol == null) {
                                        // Drag symbol in logical units
                                        val deltaLogicalX = (dragDelta.x / (renderedWidthPx * currentZoom)) * pageW
                                        val deltaLogicalY = (dragDelta.y / (renderedHeightPx * currentZoom)) * pageH
                                        onSymbolDrag(deltaLogicalX, deltaLogicalY)
                                    } else {
                                        // Pan the canvas
                                        currentPanX += dragDelta.x
                                        currentPanY += dragDelta.y
                                        onZoomAndPanChange(currentZoom, currentPanX, currentPanY)
                                    }
                                }
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Rendered Sheet
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = zoomScale
                        scaleY = zoomScale
                        translationX = panOffsetX
                        translationY = panOffsetY
                    }
                    .size(renderedWidthDp, renderedHeightDp)
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(2.dp))
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFFFFDF9))
                    .testTag("score_sheet_paper")
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawIntoCanvas { composeCanvas ->
                        val canvas = composeCanvas.nativeCanvas
                        canvas.save()
                        canvas.scale(fitScale, fitScale)

                        ScoreRenderer.drawPage(
                            canvas = canvas,
                            page = page,
                            selectedSymbolIds = selectedSymbolIds,
                            showGuides = showGuides,
                            highlightLine4 = highlightLine4
                        )

                        canvas.restore()
                    }
                }
            }
        }
    }
}
