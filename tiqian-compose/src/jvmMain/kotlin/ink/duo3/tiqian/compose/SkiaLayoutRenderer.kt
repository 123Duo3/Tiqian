package ink.duo3.tiqian.compose

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import ink.duo3.tiqian.core.LayoutResult
import ink.duo3.tiqian.shaping.skia.SkiaSystemTypefaces
import ink.duo3.tiqian.shaping.skia.drawTiqianGlyphs
import ink.duo3.tiqian.shaping.skia.shapeTextBlob
import org.jetbrains.skia.Font
import org.jetbrains.skia.Paint
import org.jetbrains.skia.shaper.Shaper

/**
 * Draws a [LayoutResult] onto the Compose desktop canvas. Pure presentation:
 * x stepping comes from cluster advances the ENGINE resolved; glyphs come
 * from the shared language-tagged blob path ([shapeTextBlob]) so forms match
 * what the engine measured. The cluster walk (autospace strips, line-edge
 * gap suppression) is the same contract the playground raster implements.
 */
internal fun DrawScope.drawTiqianLayout(
    result: LayoutResult,
    color: Int = 0xFF000000.toInt(),
) {
    val fontSize = result.input.textStyle.fontSize
    val language = result.input.textStyle.locale
    val cjkFont = Font(SkiaSystemTypefaces.cjk, fontSize)
    val latinFont = Font(SkiaSystemTypefaces.latin, fontSize)
    val paint = Paint().apply { this.color = color }
    val shaper = Shaper.makeShaperDrivenWrapper()

    drawIntoCanvas { canvas ->
        val skCanvas = canvas.nativeCanvas
        // Shared cluster-walk (tiqian-shaping-skia) — same path the playground
        // raster uses, so the role-containment / leading-shift handling can't
        // drift between the two.
        drawTiqianGlyphs(skCanvas, result, cjkFont, latinFont, paint, shaper)

        // Emphasis dots (ADR 0018): the dot glyph's ink centre lands on the
        // engine-decided anchor.
        val appliedDots = result.debug.decorationDecisions.filter { it.applied }
        if (appliedDots.isNotEmpty()) {
            val dotGlyph = cjkFont.getUTF32Glyph(EMPHASIS_DOT.code)
            val dotInk = cjkFont.getBounds(shortArrayOf(dotGlyph)).firstOrNull()
            val dotBlob = shapeTextBlob(shaper, EMPHASIS_DOT.toString(), cjkFont, language)
            if (dotBlob != null && dotInk != null) {
                val inkCenterX = (dotInk.left + dotInk.right) / 2f
                val inkCenterY = (dotInk.top + dotInk.bottom) / 2f
                for (dot in appliedDots) {
                    skCanvas.drawTextBlob(dotBlob, dot.anchorX - inkCenterX, dot.anchorY - inkCenterY, paint)
                }
            }
        }

        // Decoration segments (ADR 0018/0024): 示亡号 frames (continuation
        // edges stay undrawn), 专名号 straight underlines, 书名号甲式 wavy
        // underlines.
        if (result.debug.decorationSegments.isNotEmpty()) {
            val framePaint = Paint().apply {
                this.color = color
                mode = org.jetbrains.skia.PaintMode.STROKE
                strokeWidth = (fontSize / 16f).coerceAtLeast(1f)
            }
            for (seg in result.debug.decorationSegments) {
                when (seg.kind) {
                    "ProperNoun" ->
                        skCanvas.drawLine(seg.left, seg.top, seg.right, seg.top, framePaint)
                    "BookTitle" -> {
                        val path = ink.duo3.tiqian.shaping.skia.wavyLinePath(seg.left, seg.right, seg.top, fontSize)
                        skCanvas.drawPath(path, framePaint)
                    }
                    else -> {
                        skCanvas.drawLine(seg.left, seg.top, seg.right, seg.top, framePaint)
                        skCanvas.drawLine(seg.left, seg.bottom, seg.right, seg.bottom, framePaint)
                        if (!seg.openStart) skCanvas.drawLine(seg.left, seg.top, seg.left, seg.bottom, framePaint)
                        if (!seg.openEnd) skCanvas.drawLine(seg.right, seg.top, seg.right, seg.bottom, framePaint)
                    }
                }
            }
        }
    }
}

/** CLREQ 着重号 glyph: U+2022 BULLET (CLREQ allows U+25CF or U+2022). */
private const val EMPHASIS_DOT = '•'

