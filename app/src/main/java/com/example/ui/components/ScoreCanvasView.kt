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
import androidx.compose.runtime.rememberUpdatedState
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

    val currentZoomScale by rememberUpdatedState(zoomScale)
    val currentPanOffsetX by rememberUpdatedState(panOffsetX)
    val currentPanOffsetY by rememberUpdatedState(panOffsetY)
    val currentSelectedSymbolIds by rememberUpdatedState(selectedSymbolIds)
    val currentActivePaletteSymbol by rememberUpdatedState(activePaletteSymbol)
    val currentActiveCustomSymbol by rememberUpdatedState(activeCustomSymbol)
    val currentOnZoomAndPanChange by rememberUpdatedState(onZoomAndPanChange)
    val currentOnCanvasTap by rememberUpdatedState(onCanvasTap)
    val currentOnSymbolDrag by rememberUpdatedState(onSymbolDrag)
    val currentOnSymbolDragFinished by rememberUpdatedState(onSymbolDragFinished)

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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(page.id) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val downPos = down.position
                        var currentZoom = currentZoomScale
                        var currentPanX = currentPanOffsetX
                        var currentPanY = currentPanOffsetY

                        val (downLogicalX, downLogicalY) = com.example.editor.ScoreTransformMath.screenToLogical(
                            screenX = downPos.x,
                            screenY = downPos.y,
                            containerWidthPx = containerWidthPx,
                            containerHeightPx = containerHeightPx,
                            renderedWidthPx = renderedWidthPx,
                            renderedHeightPx = renderedHeightPx,
                            pageW = pageW,
                            pageH = pageH,
                            zoomScale = currentZoom,
                            panOffsetX = currentPanX,
                            panOffsetY = currentPanY
                        )

                        // Check if down was on a selected symbol or any symbol on page (taking rotation into account)
                        val hitSymbol = page.symbols.lastOrNull { sym ->
                            val (hw, hh) = sym.getVisualBounds(page.lineSpacing)
                            if (sym.rotation != 0f) {
                                val dx = (downLogicalX - sym.x).toDouble()
                                val dy = (downLogicalY - sym.y).toDouble()
                                val rad = Math.toRadians(-sym.rotation.toDouble())
                                val cos = Math.cos(rad)
                                val sin = Math.sin(rad)
                                val localX = (dx * cos - dy * sin).toFloat()
                                val localY = (dx * sin + dy * cos).toFloat()
                                localX in (-hw - 14f)..(hw + 14f) && localY in (-hh - 14f)..(hh + 14f)
                            } else {
                                downLogicalX in (sym.x - hw - 14f)..(sym.x + hw + 14f) &&
                                downLogicalY in (sym.y - hh - 14f)..(sym.y + hh + 14f)
                            }
                        }


                        var isTransforming = false
                        var isDragging = false
                        val isSymbolDrag = (hitSymbol != null || currentSelectedSymbolIds.isNotEmpty()) &&
                                           currentActivePaletteSymbol == null && currentActiveCustomSymbol == null

                        var prevCentroid = downPos
                        var prevDistance = 0f
                        var lastSingleFingerPos = downPos
                        var totalDragDistance = 0f
                        var prevPointerCount = 1

                        while (true) {
                            val event = awaitPointerEvent()
                            val activePointers = event.changes.filter { it.pressed }
                            val pointerCount = activePointers.size

                            if (pointerCount == 0) {
                                // All pointers released
                                if (!isDragging && !isTransforming && totalDragDistance < 12f) {
                                    // Clean tap on canvas
                                    currentOnCanvasTap(downLogicalX, downLogicalY)
                                } else if (isDragging && isSymbolDrag && !isTransforming) {
                                    currentOnSymbolDragFinished()
                                }
                                break
                            }

                            // Handle transitions between single-finger and multi-finger safely without jumps
                            if (pointerCount != prevPointerCount) {
                                if (pointerCount >= 2) {
                                    isTransforming = true
                                    isDragging = false
                                    val p1 = activePointers[0].position
                                    val p2 = activePointers[1].position
                                    prevDistance = (p1 - p2).getDistance()
                                    prevCentroid = (p1 + p2) / 2f
                                } else if (pointerCount == 1) {
                                    isTransforming = false
                                    lastSingleFingerPos = activePointers[0].position
                                }
                                prevPointerCount = pointerCount
                                event.changes.forEach { it.consume() }
                                continue
                            }

                            if (pointerCount >= 2) {
                                // Two fingers: Smooth pinch-to-zoom and focal-point pan
                                isTransforming = true
                                isDragging = false
                                val p1 = activePointers[0].position
                                val p2 = activePointers[1].position
                                val currentDistance = (p1 - p2).getDistance()
                                val currentCentroid = (p1 + p2) / 2f

                                if (prevDistance > 5f && currentDistance > 5f) {
                                    val zoomFactor = currentDistance / prevDistance
                                    if (zoomFactor.isFinite() && zoomFactor > 0f) {
                                        val newZoom = (currentZoom * zoomFactor).coerceIn(
                                            com.example.editor.ScoreTransformMath.MIN_ZOOM,
                                            com.example.editor.ScoreTransformMath.MAX_ZOOM
                                        )
                                        val panDelta = currentCentroid - prevCentroid

                                        val (anchoredPanX, anchoredPanY) = com.example.editor.ScoreTransformMath.calculateZoomAnchor(
                                            oldZoom = currentZoom,
                                            newZoom = newZoom,
                                            oldPanX = currentPanX,
                                            oldPanY = currentPanY,
                                            centroidX = prevCentroid.x,
                                            centroidY = prevCentroid.y,
                                            containerWidthPx = containerWidthPx,
                                            containerHeightPx = containerHeightPx,
                                            panDeltaX = panDelta.x,
                                            panDeltaY = panDelta.y
                                        )

                                        val (clampedX, clampedY) = com.example.editor.ScoreTransformMath.clampPan(
                                            panX = anchoredPanX,
                                            panY = anchoredPanY,
                                            containerWidthPx = containerWidthPx,
                                            containerHeightPx = containerHeightPx,
                                            renderedWidthPx = renderedWidthPx,
                                            renderedHeightPx = renderedHeightPx,
                                            zoomScale = newZoom
                                        )

                                        currentZoom = newZoom
                                        currentPanX = clampedX
                                        currentPanY = clampedY

                                        currentOnZoomAndPanChange(currentZoom, currentPanX, currentPanY)
                                    }
                                }

                                prevDistance = currentDistance
                                prevCentroid = currentCentroid
                                event.changes.forEach { it.consume() }
                            } else if (pointerCount == 1) {
                                val change = activePointers[0]
                                val currentPos = change.position
                                val dragDelta = currentPos - lastSingleFingerPos
                                lastSingleFingerPos = currentPos
                                totalDragDistance += dragDelta.getDistance()

                                if (totalDragDistance > 12f) {
                                    isDragging = true
                                    change.consume()

                                    if (isSymbolDrag && !isTransforming) {
                                        // Drag symbol in logical score coordinates
                                        val deltaLogicalX = (dragDelta.x / (renderedWidthPx * currentZoom)) * pageW
                                        val deltaLogicalY = (dragDelta.y / (renderedHeightPx * currentZoom)) * pageH
                                        currentOnSymbolDrag(deltaLogicalX, deltaLogicalY)
                                    } else {
                                        // Pan the canvas
                                        val (clampedX, clampedY) = com.example.editor.ScoreTransformMath.clampPan(
                                            panX = currentPanX + dragDelta.x,
                                            panY = currentPanY + dragDelta.y,
                                            containerWidthPx = containerWidthPx,
                                            containerHeightPx = containerHeightPx,
                                            renderedWidthPx = renderedWidthPx,
                                            renderedHeightPx = renderedHeightPx,
                                            zoomScale = currentZoom
                                        )
                                        currentPanX = clampedX
                                        currentPanY = clampedY
                                        currentOnZoomAndPanChange(currentZoom, currentPanX, currentPanY)
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
