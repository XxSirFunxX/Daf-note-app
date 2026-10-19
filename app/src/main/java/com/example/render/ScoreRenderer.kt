package com.example.render

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import com.example.model.PaperSize
import com.example.model.ScorePage
import com.example.model.ScoreSymbol
import com.example.model.StaffSystem
import com.example.model.SymbolCategory
import com.example.model.SymbolType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object ScoreRenderer {

    // Reusable Paints for performance
    private val staffPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#25221F")
        strokeWidth = 1.8f
        style = Paint.Style.STROKE
    }

    private val staffLine4Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8C351B") // Distinguished accent for Line 4 (Ring sound line)
        strokeWidth = 2.2f
        style = Paint.Style.STROKE
    }

    private val inkFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1C1A18")
        style = Paint.Style.FILL
    }

    private val inkStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1C1A18")
        strokeWidth = 2.0f
        style = Paint.Style.STROKE
    }

    private val paperPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFDF9")
        style = Paint.Style.FILL
    }

    private val textHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1C1A18")
        textSize = 28f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    private val textSubtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#504A44")
        textSize = 16f
        typeface = Typeface.DEFAULT
        textAlign = Paint.Align.CENTER
    }

    private val textNumberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1C1A18")
        textSize = 22f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    private val selectionBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E08A1E")
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private val selectionHandlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E08A1E")
        style = Paint.Style.FILL
    }

    /**
     * Renders the complete score page onto any standard Android Canvas.
     */
    fun drawPage(
        canvas: Canvas,
        page: ScorePage,
        selectedSymbolIds: Set<String> = emptySet(),
        showGuides: Boolean = true,
        highlightLine4: Boolean = true
    ) {
        val width = page.paperSize.widthPt
        val height = page.paperSize.heightPt

        // 1. Paper Background
        canvas.drawRect(0f, 0f, width, height, paperPaint)

        // 2. Page Header / Title
        if (page.title.isNotBlank()) {
            canvas.drawText(page.title, width / 2f, page.marginTop * 0.45f, textHeaderPaint)
        }
        if (page.subtitle.isNotBlank()) {
            canvas.drawText(page.subtitle, width / 2f, page.marginTop * 0.72f, textSubtitlePaint)
        }
        if (page.lessonNumber.isNotBlank()) {
            // Draw lesson number badge
            val badgeX = page.marginLeft + 25f
            val badgeY = page.marginTop * 0.5f
            val badgeRadius = 18f
            canvas.drawCircle(badgeX, badgeY, badgeRadius, inkStrokePaint)
            val badgeTextPaint = Paint(textNumberPaint).apply { textSize = 18f }
            canvas.drawText(page.lessonNumber, badgeX, badgeY + 6f, badgeTextPaint)
        }

        // 3. Staves
        val staves = page.generateStaves()
        for (staff in staves) {
            drawStaffSystem(canvas, staff, highlightLine4, showGuides)
        }

        // 4. Symbols
        for (symbol in page.symbols) {
            drawSymbol(canvas, symbol, page.lineSpacing)

            // Draw selection box and handles if selected
            if (selectedSymbolIds.contains(symbol.id)) {
                drawSelectionHighlight(canvas, symbol, page.lineSpacing)
            }
        }

        // 5. Page Number Footer
        val footerPaint = Paint(textSubtitlePaint).apply {
            textSize = 14f
            textAlign = Paint.Align.CENTER
        }
        val footerText = "صفحه ${page.pageNumber}"
        canvas.drawText(footerText, width / 2f, height - (page.marginBottom * 0.35f), footerPaint)
    }

    fun drawSingleSymbolPreview(canvas: Canvas, type: SymbolType, baseSpacing: Float = 14f) {
        val dummy = ScoreSymbol(type = type, x = 0f, y = 0f, scale = 1.0f)
        drawSymbol(canvas, dummy, baseSpacing)
    }

    private fun drawStaffSystem(
        canvas: Canvas,
        staff: StaffSystem,
        highlightLine4: Boolean,
        showGuides: Boolean
    ) {
        // Line 1 (Bottom line - Bass / Tom)
        canvas.drawLine(staff.left, staff.line1Y, staff.right, staff.line1Y, staffPaint)

        // Line 2 (Middle)
        canvas.drawLine(staff.left, staff.line2Y, staff.right, staff.line2Y, staffPaint)

        // Line 3
        canvas.drawLine(staff.left, staff.line3Y, staff.right, staff.line3Y, staffPaint)

        // Line 4 (Top line - Bak / Ring sound line)
        val l4Paint = if (highlightLine4) staffLine4Paint else staffPaint
        canvas.drawLine(staff.left, staff.line4Y, staff.right, staff.line4Y, l4Paint)

        // Left clef/bar bracket
        val clefWidth = 6f
        canvas.drawRect(staff.left, staff.line4Y, staff.left + clefWidth, staff.line1Y, inkFillPaint)
        canvas.drawLine(staff.left + 12f, staff.line4Y, staff.left + 12f, staff.line1Y, staffPaint)

        // Right final closing line
        canvas.drawLine(staff.right, staff.line4Y, staff.right, staff.line1Y, staffPaint)

        if (showGuides) {
            val guidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#B0A69A")
                textSize = 10f
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("۴ (حلقه‌ها)", staff.left - 8f, staff.line4Y + 3f, guidePaint)
            canvas.drawText("۱ (تُم)", staff.left - 8f, staff.line1Y + 3f, guidePaint)
        }
    }

    private fun drawSymbol(canvas: Canvas, symbol: ScoreSymbol, baseSpacing: Float) {
        canvas.save()
        canvas.translate(symbol.x, symbol.y)
        if (symbol.rotation != 0f) {
            canvas.rotate(symbol.rotation)
        }
        val scale = symbol.scale * (baseSpacing / 16f)
        canvas.scale(scale, scale)

        when (symbol.type) {
            // Notes
            SymbolType.NOTE_QUARTER -> drawQuarterNote(canvas, symbol.isStemUp)
            SymbolType.NOTE_HALF -> drawHalfNote(canvas, symbol.isStemUp)
            SymbolType.NOTE_WHOLE -> drawWholeNote(canvas)
            SymbolType.NOTE_EIGHTH -> drawEighthNote(canvas, symbol.isStemUp)
            SymbolType.NOTE_SIXTEENTH -> drawSixteenthNote(canvas, symbol.isStemUp)
            SymbolType.NOTE_THIRTYSECOND -> drawThirtySecondNote(canvas, symbol.isStemUp)
            SymbolType.NOTE_SIXTYFOURTH -> drawSixtyFourthNote(canvas, symbol.isStemUp)

            // Rests
            SymbolType.REST_QUARTER -> drawQuarterRest(canvas)
            SymbolType.REST_EIGHTH -> drawEighthRest(canvas)
            SymbolType.REST_SIXTEENTH -> drawSixteenthRest(canvas)
            SymbolType.REST_HALF -> drawHalfRest(canvas)
            SymbolType.REST_WHOLE -> drawWholeRest(canvas)
            SymbolType.REST_THIRTYSECOND -> drawThirtySecondRest(canvas)
            SymbolType.REST_SIXTYFOURTH -> drawSixtyFourthRest(canvas)

            // Ring Sounds (صدای حلقه‌ها - خط چهارم)
            SymbolType.RING_SINGLE -> drawRingSingle(canvas)
            SymbolType.RING_TREMOLO -> drawRingTremolo(canvas)
            SymbolType.RING_ACCENT -> drawRingAccent(canvas)
            SymbolType.RING_DOUBLE -> drawRingDouble(canvas)
            SymbolType.RING_THUMB_ROLL -> drawRingThumbRoll(canvas)

            // Techniques & Strokes
            SymbolType.TECH_RAST -> drawTechRast(canvas)
            SymbolType.TECH_CHAP -> drawTechChap(canvas)
            SymbolType.TECH_TOM -> drawTechText(canvas, "تُم")
            SymbolType.TECH_BAK -> drawTechText(canvas, "بَک")
            SymbolType.TECH_MIANEH -> drawTechText(canvas, "میانه")
            SymbolType.TECH_RIZ -> drawTechRiz(canvas)
            SymbolType.TECH_ESHAREH -> drawTechEshareh(canvas)
            SymbolType.TECH_PALANG -> drawTechPalang(canvas)
            SymbolType.TECH_SEVEN -> drawTechText(canvas, "۷")
            SymbolType.TECH_EIGHT -> drawTechText(canvas, "۸")

            // Bar lines
            SymbolType.BAR_SINGLE -> drawBarSingle(canvas, baseSpacing)
            SymbolType.BAR_DOUBLE -> drawBarDouble(canvas, baseSpacing)
            SymbolType.BAR_FINAL -> drawBarFinal(canvas, baseSpacing)
            SymbolType.BAR_REPEAT_START -> drawBarRepeat(canvas, baseSpacing, isStart = true)
            SymbolType.BAR_REPEAT_END -> drawBarRepeat(canvas, baseSpacing, isStart = false)

            // Time Signatures
            SymbolType.TIME_2_4 -> drawTimeSignature(canvas, "2", "4", baseSpacing)
            SymbolType.TIME_3_4 -> drawTimeSignature(canvas, "3", "4", baseSpacing)
            SymbolType.TIME_4_4 -> drawTimeSignature(canvas, "4", "4", baseSpacing)
            SymbolType.TIME_6_8 -> drawTimeSignature(canvas, "6", "8", baseSpacing)
            SymbolType.TIME_7_8 -> drawTimeSignature(canvas, "7", "8", baseSpacing)
            SymbolType.TIME_CUSTOM -> {
                val parts = (symbol.textValue ?: "2/4").split("/")
                val num = parts.getOrNull(0) ?: "2"
                val den = parts.getOrNull(1) ?: "4"
                drawTimeSignature(canvas, num, den, baseSpacing)
            }

            // Tempo & Dynamic text
            SymbolType.TEXT_TEMPO_ANDANTE -> drawTempoText(canvas, symbol.textValue ?: "Andante ♩=80")
            SymbolType.TEXT_DYNAMIC_F -> drawDynamicText(canvas, "f")
            SymbolType.TEXT_DYNAMIC_P -> drawDynamicText(canvas, "p")
            SymbolType.TEXT_DYNAMIC_MF -> drawDynamicText(canvas, "mf")
            SymbolType.TEXT_LESSON_BADGE -> drawLessonBadge(canvas, symbol.textValue ?: "۱")

            // Custom
            SymbolType.CUSTOM_SYMBOL -> drawCustomSymbol(canvas, symbol)
        }

        canvas.restore()
    }

    private fun drawSelectionHighlight(canvas: Canvas, symbol: ScoreSymbol, baseSpacing: Float) {
        val (halfW, halfH) = symbol.getVisualBounds(baseSpacing)
        val left = symbol.x - halfW - 4f
        val top = symbol.y - halfH - 4f
        val right = symbol.x + halfW + 4f
        val bottom = symbol.y + halfH + 4f

        canvas.drawRoundRect(left, top, right, bottom, 6f, 6f, selectionBoxPaint)

        // Draw 4 corner handles
        val handleR = 5f
        canvas.drawCircle(left, top, handleR, selectionHandlePaint)
        canvas.drawCircle(right, top, handleR, selectionHandlePaint)
        canvas.drawCircle(left, bottom, handleR, selectionHandlePaint)
        canvas.drawCircle(right, bottom, handleR, selectionHandlePaint)
    }

    // ==========================================
    // GLYPH DRAWING IMPLEMENTATIONS
    // ==========================================

    private fun drawNoteHead(canvas: Canvas, filled: Boolean) {
        canvas.save()
        canvas.rotate(-22f)
        val rect = RectF(-7.5f, -5.5f, 7.5f, 5.5f)
        if (filled) {
            canvas.drawOval(rect, inkFillPaint)
        } else {
            val hollowPaint = Paint(inkStrokePaint).apply { strokeWidth = 2.4f }
            canvas.drawOval(rect, hollowPaint)
        }
        canvas.restore()
    }

    private fun drawQuarterNote(canvas: Canvas, isStemUp: Boolean) {
        drawNoteHead(canvas, filled = true)
        val stemLength = 34f
        val stemPaint = Paint(inkStrokePaint).apply { strokeWidth = 2f }
        if (isStemUp) {
            canvas.drawLine(6.5f, 0f, 6.5f, -stemLength, stemPaint)
        } else {
            canvas.drawLine(-6.5f, 0f, -6.5f, stemLength, stemPaint)
        }
    }

    private fun drawHalfNote(canvas: Canvas, isStemUp: Boolean) {
        drawNoteHead(canvas, filled = false)
        val stemLength = 34f
        val stemPaint = Paint(inkStrokePaint).apply { strokeWidth = 2f }
        if (isStemUp) {
            canvas.drawLine(6.5f, 0f, 6.5f, -stemLength, stemPaint)
        } else {
            canvas.drawLine(-6.5f, 0f, -6.5f, stemLength, stemPaint)
        }
    }

    private fun drawWholeNote(canvas: Canvas) {
        canvas.save()
        canvas.rotate(-10f)
        val rect = RectF(-9f, -6f, 9f, 6f)
        val hollowPaint = Paint(inkStrokePaint).apply { strokeWidth = 2.8f }
        canvas.drawOval(rect, hollowPaint)
        canvas.restore()
    }

    private fun drawEighthNote(canvas: Canvas, isStemUp: Boolean) {
        drawQuarterNote(canvas, isStemUp)
        val flagPath = Path()
        if (isStemUp) {
            flagPath.moveTo(6.5f, -34f)
            flagPath.cubicTo(14f, -28f, 16f, -18f, 13f, -10f)
            flagPath.cubicTo(10f, -16f, 10f, -24f, 6.5f, -28f)
            flagPath.close()
        } else {
            flagPath.moveTo(-6.5f, 34f)
            flagPath.cubicTo(1f, 28f, 3f, 18f, 0f, 10f)
            flagPath.cubicTo(-3f, 16f, -3f, 24f, -6.5f, 28f)
            flagPath.close()
        }
        canvas.drawPath(flagPath, inkFillPaint)
    }

    private fun drawSixteenthNote(canvas: Canvas, isStemUp: Boolean) {
        drawEighthNote(canvas, isStemUp)
        // Add second flag
        val flag2 = Path()
        if (isStemUp) {
            flag2.moveTo(6.5f, -26f)
            flag2.cubicTo(13f, -20f, 15f, -12f, 12f, -4f)
            flag2.cubicTo(9f, -10f, 9f, -18f, 6.5f, -20f)
            flag2.close()
        } else {
            flag2.moveTo(-6.5f, 26f)
            flag2.cubicTo(0f, 20f, 2f, 12f, -1f, 4f)
            flag2.cubicTo(-4f, 10f, -4f, 18f, -6.5f, 20f)
            flag2.close()
        }
        canvas.drawPath(flag2, inkFillPaint)
    }

    private fun drawThirtySecondNote(canvas: Canvas, isStemUp: Boolean) {
        drawSixteenthNote(canvas, isStemUp)
        // Third flag line
        val flagPaint = Paint(inkStrokePaint).apply { strokeWidth = 2.2f }
        if (isStemUp) {
            canvas.drawLine(6.5f, -18f, 13f, -12f, flagPaint)
        } else {
            canvas.drawLine(-6.5f, 18f, 0f, 12f, flagPaint)
        }
    }

    private fun drawSixtyFourthNote(canvas: Canvas, isStemUp: Boolean) {
        drawThirtySecondNote(canvas, isStemUp)
        val flagPaint = Paint(inkStrokePaint).apply { strokeWidth = 2.2f }
        if (isStemUp) {
            canvas.drawLine(6.5f, -12f, 13f, -6f, flagPaint)
        } else {
            canvas.drawLine(-6.5f, 12f, 0f, 6f, flagPaint)
        }
    }

    // --- RESTS ---

    private fun drawQuarterRest(canvas: Canvas) {
        // Classical musical quarter rest zig-zag squiggle
        val path = Path().apply {
            moveTo(-2f, -18f)
            lineTo(4f, -10f)
            lineTo(-3f, -2f)
            lineTo(3f, 4f)
            cubicTo(4f, 8f, 2f, 14f, -2f, 16f)
            cubicTo(0f, 12f, 1f, 8f, -1f, 5f)
            lineTo(-4f, -1f)
            lineTo(2f, -9f)
            close()
        }
        canvas.drawPath(path, inkFillPaint)
    }

    private fun drawEighthRest(canvas: Canvas) {
        val path = Path().apply {
            moveTo(-4f, -12f)
            cubicTo(-6f, -10f, -6f, -6f, -3f, -5f)
            cubicTo(0f, -4f, 2f, -7f, 1f, -10f)
            lineTo(3f, -12f)
            lineTo(-2f, 14f)
            lineTo(-4f, 13f)
            lineTo(0f, -9f)
            close()
        }
        canvas.drawPath(path, inkFillPaint)
    }

    private fun drawSixteenthRest(canvas: Canvas) {
        drawEighthRest(canvas)
        canvas.drawCircle(-2f, 1f, 2.5f, inkFillPaint)
    }

    private fun drawThirtySecondRest(canvas: Canvas) {
        drawSixteenthRest(canvas)
        canvas.drawCircle(-3f, 7f, 2.5f, inkFillPaint)
    }

    private fun drawSixtyFourthRest(canvas: Canvas) {
        drawThirtySecondRest(canvas)
        canvas.drawCircle(-4f, 12f, 2.5f, inkFillPaint)
    }

    private fun drawHalfRest(canvas: Canvas) {
        // Sits on line
        canvas.drawRect(-8f, -7f, 8f, 0f, inkFillPaint)
    }

    private fun drawWholeRest(canvas: Canvas) {
        // Hangs below line
        canvas.drawRect(-8f, 0f, 8f, 7f, inkFillPaint)
    }

    // --- RING SOUNDS (صدای حلقه‌ها - Line 4 strictly!) ---

    private fun drawRingSingle(canvas: Canvas) {
        // Circular chime ring with radiate sparkle/stem (as seen in Tombak/Daf notation reference)
        val outerPaint = Paint(inkStrokePaint).apply {
            strokeWidth = 2.4f
            color = Color.parseColor("#993D22")
        }
        canvas.drawCircle(0f, 0f, 7.5f, outerPaint)
        canvas.drawCircle(0f, 0f, 2.5f, inkFillPaint)
        // Upward chime stem with jingle flag
        val chimePaint = Paint(inkStrokePaint).apply { strokeWidth = 1.8f; color = Color.parseColor("#993D22") }
        canvas.drawLine(0f, -7.5f, 0f, -19f, chimePaint)
        canvas.drawArc(RectF(-6f, -22f, 6f, -12f), 180f, 180f, false, chimePaint)
    }

    private fun drawRingTremolo(canvas: Canvas) {
        drawRingSingle(canvas)
        // Triple tremolo slashes across ring stem
        val tremPaint = Paint(inkStrokePaint).apply { strokeWidth = 2.2f }
        canvas.drawLine(-5f, -12f, 5f, -15f, tremPaint)
        canvas.drawLine(-5f, -16f, 5f, -19f, tremPaint)
        canvas.drawLine(-5f, -20f, 5f, -23f, tremPaint)
    }

    private fun drawRingAccent(canvas: Canvas) {
        drawRingSingle(canvas)
        // Accent mark above ring
        val accPath = Path().apply {
            moveTo(-7f, -26f)
            lineTo(0f, -21f)
            lineTo(7f, -26f)
        }
        canvas.drawPath(accPath, inkStrokePaint)
    }

    private fun drawRingDouble(canvas: Canvas) {
        val outerPaint = Paint(inkStrokePaint).apply {
            strokeWidth = 2.2f
            color = Color.parseColor("#993D22")
        }
        canvas.drawCircle(-5f, 0f, 6.5f, outerPaint)
        canvas.drawCircle(5f, 0f, 6.5f, outerPaint)
        canvas.drawLine(0f, -6.5f, 0f, -18f, outerPaint)
    }

    private fun drawRingThumbRoll(canvas: Canvas) {
        drawRingSingle(canvas)
        // Curved arrow sweep indicating finger roll
        val sweepPath = Path().apply {
            moveTo(-12f, 4f)
            quadTo(0f, 14f, 12f, 4f)
        }
        val sweepPaint = Paint(inkStrokePaint).apply { strokeWidth = 2f }
        canvas.drawPath(sweepPath, sweepPaint)
        // Arrow head
        canvas.drawLine(12f, 4f, 8f, 3f, sweepPaint)
        canvas.drawLine(12f, 4f, 10f, 8f, sweepPaint)
    }

    // --- TECHNIQUES & STROKES ---

    private fun drawTechRast(canvas: Canvas) {
        // Downward chevron ∨ (راست)
        val path = Path().apply {
            moveTo(-6f, -8f)
            lineTo(0f, 0f)
            lineTo(6f, -8f)
        }
        val paint = Paint(inkStrokePaint).apply { strokeWidth = 2.5f }
        canvas.drawPath(path, paint)
    }

    private fun drawTechChap(canvas: Canvas) {
        // Upward chevron ∧ (چپ)
        val path = Path().apply {
            moveTo(-6f, 8f)
            lineTo(0f, 0f)
            lineTo(6f, 8f)
        }
        val paint = Paint(inkStrokePaint).apply { strokeWidth = 2.5f }
        canvas.drawPath(path, paint)
    }

    private fun drawTechText(canvas: Canvas, text: String) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1C1A18")
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(text, 0f, 4f, paint)
    }

    private fun drawTechRiz(canvas: Canvas) {
        // Wavy ribbon for roll / tremolo ~~~~
        val path = Path().apply {
            moveTo(-16f, 0f)
            quadTo(-12f, -5f, -8f, 0f)
            quadTo(-4f, 5f, 0f, 0f)
            quadTo(4f, -5f, 8f, 0f)
            quadTo(12f, 5f, 16f, 0f)
        }
        val paint = Paint(inkStrokePaint).apply { strokeWidth = 2.2f }
        canvas.drawPath(path, paint)
    }

    private fun drawTechEshareh(canvas: Canvas) {
        // Small grace note symbol with diagonal slash
        canvas.save()
        canvas.scale(0.55f, 0.55f)
        drawQuarterNote(canvas, isStemUp = true)
        val slashPaint = Paint(inkStrokePaint).apply { strokeWidth = 2.5f }
        canvas.drawLine(-8f, -12f, 10f, -22f, slashPaint)
        canvas.restore()
    }

    private fun drawTechPalang(canvas: Canvas) {
        // Persian snap / flick marker (star cross ✦)
        val p = Paint(inkStrokePaint).apply { strokeWidth = 2.4f }
        canvas.drawLine(-7f, 0f, 7f, 0f, p)
        canvas.drawLine(0f, -7f, 0f, 7f, p)
        canvas.drawLine(-5f, -5f, 5f, 5f, p)
        canvas.drawLine(-5f, 5f, 5f, -5f, p)
    }

    // --- BAR LINES ---

    private fun drawBarSingle(canvas: Canvas, baseSpacing: Float) {
        val totalH = baseSpacing * 3f + (baseSpacing * 1.0f)
        val p = Paint(inkStrokePaint).apply { strokeWidth = 2f }
        canvas.drawLine(0f, -totalH / 2f, 0f, totalH / 2f, p)
    }

    private fun drawBarDouble(canvas: Canvas, baseSpacing: Float) {
        val totalH = baseSpacing * 3f + (baseSpacing * 1.0f)
        val p = Paint(inkStrokePaint).apply { strokeWidth = 1.8f }
        canvas.drawLine(-3.5f, -totalH / 2f, -3.5f, totalH / 2f, p)
        canvas.drawLine(3.5f, -totalH / 2f, 3.5f, totalH / 2f, p)
    }

    private fun drawBarFinal(canvas: Canvas, baseSpacing: Float) {
        val totalH = baseSpacing * 3f + (baseSpacing * 1.0f)
        val pThin = Paint(inkStrokePaint).apply { strokeWidth = 1.8f }
        val pThick = Paint(inkStrokePaint).apply { strokeWidth = 4.8f }
        canvas.drawLine(-5f, -totalH / 2f, -5f, totalH / 2f, pThin)
        canvas.drawLine(2f, -totalH / 2f, 2f, totalH / 2f, pThick)
    }

    private fun drawBarRepeat(canvas: Canvas, baseSpacing: Float, isStart: Boolean) {
        drawBarDouble(canvas, baseSpacing)
        val dotX = if (isStart) 8f else -8f
        canvas.drawCircle(dotX, -baseSpacing * 0.5f, 2.5f, inkFillPaint)
        canvas.drawCircle(dotX, baseSpacing * 0.5f, 2.5f, inkFillPaint)
    }

    // --- TIME SIGNATURES ---

    private fun drawTimeSignature(canvas: Canvas, numerator: String, denominator: String, baseSpacing: Float) {
        val numPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1C1A18")
            textSize = baseSpacing * 1.5f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        // Stacked numbers
        canvas.drawText(numerator, 0f, -2f, numPaint)
        canvas.drawText(denominator, 0f, baseSpacing * 1.4f, numPaint)
    }

    // --- TEXT & ANNOTATIONS ---

    private fun drawTempoText(canvas: Canvas, text: String) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1C1A18")
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText(text, 0f, 5f, paint)
    }

    private fun drawDynamicText(canvas: Canvas, mark: String) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1C1A18")
            textSize = 18f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(mark, 0f, 6f, paint)
    }

    private fun drawLessonBadge(canvas: Canvas, text: String) {
        canvas.drawCircle(0f, 0f, 14f, inkStrokePaint)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1C1A18")
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(text, 0f, 5f, paint)
    }

    private fun drawCustomSymbol(canvas: Canvas, symbol: ScoreSymbol) {
        // Draw custom geometric/symbol marker with name
        val p = Paint(inkStrokePaint).apply { strokeWidth = 2f }
        canvas.drawRoundRect(-12f, -12f, 12f, 12f, 4f, 4f, p)
        val textP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1C1A18")
            textSize = 9f
            textAlign = Paint.Align.CENTER
        }
        val label = symbol.name.take(3)
        canvas.drawText(label, 0f, 3f, textP)
    }
}
