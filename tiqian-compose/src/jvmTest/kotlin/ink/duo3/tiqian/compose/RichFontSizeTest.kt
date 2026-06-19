package ink.duo3.tiqian.compose

import ink.duo3.tiqian.clreq.ClreqProfile
import ink.duo3.tiqian.core.LayoutConstraints
import ink.duo3.tiqian.core.TextRange
import ink.duo3.tiqian.core.TextSpan
import ink.duo3.tiqian.core.TextStyle
import ink.duo3.tiqian.layout.ExplainableStubParagraphLayoutEngine
import ink.duo3.tiqian.layout.LookaheadLineBreaker
import ink.duo3.tiqian.shaping.skia.SkiaFontMetricsResolver
import ink.duo3.tiqian.shaping.skia.SkiaTextShaper
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Rich-text per-span 字号 (ADR 0030 B 档): a sized span SHAPES and is MEASURED at
 * its own size — the cluster advance scales and the line grows taller, both
 * driven through the real Skia shaper/metrics (not a render-only trick).
 */
class RichFontSizeTest {

    private val measurer = ParagraphMeasurer(
        ExplainableStubParagraphLayoutEngine(
            lineBreaker = LookaheadLineBreaker(),
            textShaper = SkiaTextShaper(),
            fontMetricsResolver = SkiaFontMetricsResolver(),
            clreqProfileResolver = { ClreqProfile.MainlandHorizontal },
        ),
    )

    @Test
    fun sizedSpanScalesAdvanceAndLineHeight() {
        val constraints = LayoutConstraints(maxWidth = 4000f)
        val base = measurer.measure("汉字测试", constraints, TextStyle(fontSize = 20f))
        val sized = measurer.measure(
            "汉字测试",
            constraints,
            TextStyle(fontSize = 20f),
            spans = listOf(TextSpan(TextRange(2, 4), TextStyle(fontSize = 40f))), // 测试 at 2×
        )

        val baseGlyph = base.clusters.first { it.range.start == 2 }.advance
        val sizedGlyph = sized.clusters.first { it.range.start == 2 }.advance
        // 测 shaped at 40 vs 20 → ~2× the advance (full-width CJK ≈ 1em either way).
        assertTrue(
            sizedGlyph in baseGlyph * 1.8f..baseGlyph * 2.2f,
            "sized 测 advance $sizedGlyph should be ~2× base $baseGlyph",
        )
        // The unsized head keeps its size.
        val baseHead = base.clusters.first { it.range.start == 0 }.advance
        val sizedHead = sized.clusters.first { it.range.start == 0 }.advance
        assertTrue(sizedHead in baseHead * 0.98f..baseHead * 1.02f, "unsized 汉 must keep its advance")
        // The 2× cluster lifts the line height (paragraph-wide max metrics).
        assertTrue(sized.size.height > base.size.height, "a 2× span must grow the line height")
    }
}
