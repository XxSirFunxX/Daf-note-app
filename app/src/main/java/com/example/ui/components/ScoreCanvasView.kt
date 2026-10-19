package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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

        var isDraggingSymbol by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                // 1. Two-finger Pinch to Zoom & Pan
                .pointerInput(page.id, zoomScale, panOffsetX, panOffsetY) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val oldZoom = zoomScale
                        val newZoom = (oldZoom * zoom).coerceIn(0.5f, 4.0f)
                        val zoomFactor = newZoom / oldZoom

                        // Zoom centered on the touch centroid
                        val newPanX = (panOffsetX + pan.x) * zoomFactor + (1f - zoomFactor) * (centroid.x - containerWidthPx / 2f)
                        val newPanY = (panOffsetY + pan.y) * zoomFactor + (1f - zoomFactor) * (centroid.y - containerHeightPx / 2f)

                        onZoomAndPanChange(newZoom, newPanX, newPanY)
                    }
                }
                // 2. Single-finger drag (symbol drag or canvas pan)
                .pointerInput(page.id, selectedSymbolIds, activePaletteSymbol, zoomScale, panOffsetX, panOffsetY) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val (lx, ly) = screenToLogical(offset.x, offset.y)
                            val hit = page.symbols.any { sym ->
                                val (hw, hh) = sym.getVisualBounds(page.lineSpacing)
                                lx in (sym.x - hw - 12f)..(sym.x + hw + 12f) &&
                                        ly in (sym.y - hh - 12f)..(sym.y + hh + 12f)
                            }
                            isDraggingSymbol = hit || selectedSymbolIds.isNotEmpty()
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (isDraggingSymbol && activePaletteSymbol == null) {
                                // Dragging symbol(s) in logical coordinates
                                val deltaLogicalX = (dragAmount.x / (renderedWidthPx * zoomScale)) * pageW
                                val deltaLogicalY = (dragAmount.y / (renderedHeightPx * zoomScale)) * pageH
                                onSymbolDrag(deltaLogicalX, deltaLogicalY)
                            } else {
                                // Panning the view
                                onZoomAndPanChange(zoomScale, panOffsetX + dragAmount.x, panOffsetY + dragAmount.y)
                            }
                        },
                        onDragEnd = {
                            if (isDraggingSymbol) {
                                isDraggingSymbol = false
                                onSymbolDragFinished()
                            }
                        },
                        onDragCancel = {
                            isDraggingSymbol = false
                        }
                    )
                }
                // 3. Single tap (place symbol or select existing)
                .pointerInput(page.id, activePaletteSymbol, zoomScale, panOffsetX, panOffsetY) {
                    detectTapGestures { offset ->
                        val (lx, ly) = screenToLogical(offset.x, offset.y)
                        onCanvasTap(lx, ly)
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
