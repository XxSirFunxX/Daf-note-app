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
import com.example.render.ScoreRenderer

@Composable
fun ScoreCanvasView(
    page: ScorePage,
    selectedSymbolIds: Set<String>,
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

        // Page logical dimension
        val pageW = page.paperSize.widthPt
        val pageH = page.paperSize.heightPt

        // Fit page comfortably inside available container space
        val horizontalScale = (containerWidthPx - 32f) / pageW
        val verticalScale = (containerHeightPx - 32f) / pageH
        val fitScale = minOf(horizontalScale, verticalScale).coerceAtLeast(0.1f)

        val renderedWidthDp = with(density) { (pageW * fitScale).toDp() }
        val renderedHeightDp = with(density) { (pageH * fitScale).toDp() }

        val renderedWidthPx = pageW * fitScale
        val renderedHeightPx = pageH * fitScale

        // Gesture state
        var isDraggingSymbol by remember { mutableStateOf(false) }

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
                // 1. Gesture detector for two-finger zoom & pan
                .pointerInput(page.id) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newZoom = (zoomScale * zoom).coerceIn(0.6f, 3.5f)
                        val newPanX = panOffsetX + pan.x
                        val newPanY = panOffsetY + pan.y
                        onZoomAndPanChange(newZoom, newPanX, newPanY)
                    }
                }
                // 2. Gesture detector for symbol dragging
                .pointerInput(page.id, selectedSymbolIds) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val logicalX = (offset.x / renderedWidthPx) * pageW
                            val logicalY = (offset.y / renderedHeightPx) * pageH

                            // Check if tapped near an existing symbol
                            val hit = page.symbols.any { sym ->
                                val (hw, hh) = sym.getVisualBounds(page.lineSpacing)
                                logicalX in (sym.x - hw - 10f)..(sym.x + hw + 10f) &&
                                        logicalY in (sym.y - hh - 10f)..(sym.y + hh + 10f)
                            }
                            if (hit) {
                                isDraggingSymbol = true
                            }
                        },
                        onDrag = { change, dragAmount ->
                            if (isDraggingSymbol || selectedSymbolIds.isNotEmpty()) {
                                change.consume()
                                val deltaLogicalX = (dragAmount.x / renderedWidthPx) * pageW
                                val deltaLogicalY = (dragAmount.y / renderedHeightPx) * pageH
                                onSymbolDrag(deltaLogicalX, deltaLogicalY)
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
                // 3. Tap gesture detector for symbol selection or placement
                .pointerInput(page.id) {
                    detectTapGestures { offset ->
                        val logicalX = (offset.x / renderedWidthPx) * pageW
                        val logicalY = (offset.y / renderedHeightPx) * pageH
                        onCanvasTap(logicalX, logicalY)
                    }
                }
                .testTag("score_sheet_paper")
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawIntoCanvas { composeCanvas ->
                    val canvas = composeCanvas.nativeCanvas
                    canvas.save()
                    // Scale native canvas to match fit scale
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
