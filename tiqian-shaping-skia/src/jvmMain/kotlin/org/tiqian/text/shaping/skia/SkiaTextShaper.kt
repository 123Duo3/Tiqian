package org.tiqian.text.shaping.skia

import org.jetbrains.skia.Font
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.FontStyle
import org.jetbrains.skia.TextLine
import org.jetbrains.skia.Typeface
import org.tiqian.text.core.Cluster
import org.tiqian.text.core.Glyph
import org.tiqian.text.core.GlyphRun
import org.tiqian.text.core.Rect
import org.tiqian.text.core.ShapingDecisionInfo
import org.tiqian.text.font.FontRole
import org.tiqian.text.shaping.ShapingInput
import org.tiqian.text.shaping.ShapingResult
import org.tiqian.text.shaping.ShapingSource
import org.tiqian.text.shaping.TextShaper
import kotlin.math.max

/**
 * Skia (Skiko) shaping adapter — the second real-measurement adapter next to
 * `AwtTextShaper` (ADR 0013). Same contract: consume the layout-decided
 * `displayText` with a single resolved font, emit one cluster + one glyph run
 * with real advances and glyph-local ink bounds. No fallback, no CLREQ
 * substitution, no punctuation decisions — those stay upstream.
 *
 * Glyph bounds come from [Font.getBounds] and are already glyph-local
 * (origin at the glyph's pen position, negative top above the baseline),
 * matching the convention `AwtTextShaper` produces via origin subtraction.
 */
class SkiaTextShaper(
    private val fontResolver: SkiaFontResolver = SystemSkiaFontResolver(),
) : TextShaper {
    override fun shape(input: ShapingInput): ShapingResult {
        val sourceText = input.text.substring(input.range.start, input.range.end)
        val displayText = input.displayText
        val font = fontResolver.resolve(input)
        val line = TextLine.make(displayText, font)
        val glyphIds = line.glyphs
        val positions = line.positions
        val glyphCount = glyphIds.size
        val advance = line.width

        val cluster = Cluster(
            range = input.range,
            text = sourceText,
            displayText = displayText,
            fontKey = input.fontDecision.candidate.key,
            advance = advance,
        )
        val inkBounds = font.getBounds(glyphIds)
        val glyphs = (0 until glyphCount).map { glyphIndex ->
            val startX = positions[2 * glyphIndex]
            val endX = if (glyphIndex + 1 < glyphCount) positions[2 * (glyphIndex + 1)] else advance
            Glyph(
                id = glyphIds[glyphIndex].toUShort().toUInt(),
                clusterRange = input.range,
                advance = max(0f, endX - startX),
                bounds = inkBounds.getOrNull(glyphIndex)?.toGlyphLocalRectOrNull(),
            )
        }
        val run = GlyphRun(
            range = input.range,
            fontKey = input.fontDecision.candidate.key,
            glyphs = glyphs,
            advance = advance,
        )
        val decision = ShapingDecisionInfo(
            range = input.range,
            sourceText = sourceText,
            displayText = displayText,
            fontKey = input.fontDecision.candidate.key,
            glyphCount = glyphCount,
            advance = advance,
            source = ShapingSource.Skia.name,
            reason = "SkiaTextShaper:${font.typeface?.familyName ?: "default"}",
            glyphsWithoutInkBounds = glyphs.count { it.bounds == null },
        )
        return ShapingResult(
            clusters = listOf(cluster),
            glyphRuns = listOf(run),
            decisions = listOf(decision),
        )
    }
}

interface SkiaFontResolver {
    fun resolve(input: ShapingInput): Font
}

/**
 * Probes [FontMgr.default] for the first available CJK / Latin family from
 * the same preference lists `SystemAwtFontResolver` uses, so AWT ↔ Skia
 * comparisons measure the same physical font whenever possible.
 *
 * Named heuristic: `SystemSkiaFontProbe`.
 */
class SystemSkiaFontResolver(
    private val fontMgr: FontMgr = FontMgr.default,
) : SkiaFontResolver {
    private val cjkTypeface: Typeface? =
        CJK_CANDIDATES.firstNotNullOfOrNull { fontMgr.matchFamilyStyle(it, FontStyle.NORMAL) }
    private val latinTypeface: Typeface? =
        LATIN_CANDIDATES.firstNotNullOfOrNull { fontMgr.matchFamilyStyle(it, FontStyle.NORMAL) }

    override fun resolve(input: ShapingInput): Font {
        val requestedFamily = input.style.fontFamilies.firstOrNull()
            ?: input.fontDecision.candidate.family.takeUnless { it == input.fontDecision.candidate.key }
        val typeface = requestedFamily?.let { fontMgr.matchFamilyStyle(it, FontStyle.NORMAL) }
            ?: input.fontDecision.role.resolvedTypeface()
        return Font(typeface, input.style.fontSize)
    }

    private fun FontRole.resolvedTypeface(): Typeface? =
        when (this) {
            FontRole.CjkText,
            FontRole.CjkPunctuation,
            -> cjkTypeface

            FontRole.LatinText,
            FontRole.Symbol,
            FontRole.Emoji,
            FontRole.Unknown,
            -> latinTypeface
        }

    companion object {
        /** Ordered by preference; first match wins. Mirrors `SystemAwtFontProbe`. */
        private val CJK_CANDIDATES = listOf(
            "Source Han Sans CN",
            "Source Han Sans CN VF",
            "Noto Sans CJK SC",
            "PingFang SC",
            "Hiragino Sans GB",
            "Sarasa UI SC",
            "Heiti SC",
            "STHeiti",
        )

        private val LATIN_CANDIDATES = listOf(
            "Inter Variable",
            "Inter",
            "SF Pro Text",
            "SF Pro",
            "Roboto",
            "Helvetica Neue",
        )
    }
}

private fun org.jetbrains.skia.Rect.toGlyphLocalRectOrNull(): Rect? {
    if (width <= 0f || height <= 0f) return null
    return Rect(
        left = left,
        top = top,
        right = right,
        bottom = bottom,
    )
}
