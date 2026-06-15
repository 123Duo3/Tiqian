package ink.duo3.tiqian.shaping.skia

import ink.duo3.tiqian.font.FontMetricSource
import ink.duo3.tiqian.font.FontMetricsRequest
import ink.duo3.tiqian.font.FontMetricsResolver
import ink.duo3.tiqian.font.FontRole
import ink.duo3.tiqian.font.RawFontMetrics
import org.jetbrains.skia.Font
import org.jetbrains.skia.Typeface

/**
 * Real Skia metrics resolver (ADR 0002 amendment). Reads the resolved
 * typeface's DECLARED metrics instead of synthesizing a box:
 *
 * - hhea-derived ascent/descent/leading from [Font.metrics] (the inflated box,
 *   kept for the no-`OS/2` fallback and overflow clamping);
 * - the font's typographic box from the `OS/2` table (`sTypoAscender` /
 *   `sTypoDescender`) → [RawFontMetrics.typoAscent] / [typoDescent], which for
 *   CJK fonts is the clean ideographic em (Source Han 0.88 / 0.12). The
 *   `ScriptAwareFontMetricsNormalizer` lays the CJK line box on THIS, not on
 *   the synthesized 0.5/0.5 square.
 *
 * Ink-bounds sampling stays a separate bad-font fallback; it is not consulted
 * here. The `BASE`-table character face (icfb/icft) is a documented follow-up.
 */
class SkiaFontMetricsResolver(
    private val cjkTypeface: Typeface? = SkiaSystemTypefaces.cjk,
    private val latinTypeface: Typeface? = SkiaSystemTypefaces.latin,
) : FontMetricsResolver {

    override fun resolve(request: FontMetricsRequest): RawFontMetrics {
        val typeface = when (request.role) {
            FontRole.CjkText, FontRole.CjkPunctuation -> cjkTypeface
            FontRole.LatinText -> latinTypeface ?: cjkTypeface
            FontRole.Symbol, FontRole.Emoji, FontRole.Unknown -> cjkTypeface ?: latinTypeface
        } ?: return fallback(request)

        val size = request.fontSize
        val font = Font(typeface, size)
        try {
            val m = font.metrics
            val upm = typeface.unitsPerEm.takeIf { it > 0 } ?: 1000
            val scale = size / upm
            val os2 = if (typeface.tableTags.contains("OS/2")) typeface.getTableData("OS/2")?.bytes else null
            val typoAscent: Float?
            val typoDescent: Float?
            if (os2 != null && os2.size >= 72) {
                typoAscent = s16(os2, 68) * scale            // sTypoAscender (FUnits, +up)
                typoDescent = -s16(os2, 70) * scale          // sTypoDescender (FUnits, -down) -> +mag
            } else {
                typoAscent = null
                typoDescent = null
            }
            return RawFontMetrics(
                ascent = -m.ascent,   // Skia ascent is negative (above baseline) -> +mag
                descent = m.descent,  // already positive (below baseline)
                leading = m.leading,
                source = FontMetricSource.RawTables,
                typoAscent = typoAscent,
                typoDescent = typoDescent,
            )
        } finally {
            font.close()
        }
    }

    /** Mirrors `StubFontMetricsResolver` so a missing system font still lays out. */
    private fun fallback(request: FontMetricsRequest): RawFontMetrics = when (request.role) {
        FontRole.CjkText, FontRole.CjkPunctuation -> RawFontMetrics(
            ascent = request.fontSize * 1.16f,
            descent = request.fontSize * 0.288f,
            typoAscent = request.fontSize * 0.88f,
            typoDescent = request.fontSize * 0.12f,
        )
        FontRole.LatinText -> RawFontMetrics(request.fontSize * 0.8f, request.fontSize * 0.2f)
        else -> RawFontMetrics(request.fontSize * 0.9f, request.fontSize * 0.25f)
    }

    private fun u16(b: ByteArray, o: Int) = ((b[o].toInt() and 0xFF) shl 8) or (b[o + 1].toInt() and 0xFF)
    private fun s16(b: ByteArray, o: Int) = u16(b, o).toShort().toInt()
}
