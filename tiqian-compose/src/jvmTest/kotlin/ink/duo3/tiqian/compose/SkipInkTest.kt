package ink.duo3.tiqian.compose

import ink.duo3.tiqian.core.DecorationKind
import ink.duo3.tiqian.core.DecorationSpan
import ink.duo3.tiqian.core.LayoutConstraints
import ink.duo3.tiqian.core.LayoutInput
import ink.duo3.tiqian.core.TextRange
import ink.duo3.tiqian.core.TextStyle
import ink.duo3.tiqian.core.TiqianTextContent
import ink.duo3.tiqian.layout.ExplainableStubParagraphLayoutEngine
import ink.duo3.tiqian.layout.LookaheadLineBreaker
import ink.duo3.tiqian.shaping.skia.SkiaFontMetricsResolver
import ink.duo3.tiqian.shaping.skia.SkiaSystemTypefaces
import ink.duo3.tiqian.shaping.skia.SkiaTextShaper
import ink.duo3.tiqian.shaping.skia.lineInkSkipIntervals
import org.jetbrains.skia.Font
import org.jetbrains.skia.shaper.Shaper
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * `text-decoration-skip-ink` (Compose lacks it): the 行间线 must break around
 * glyph ink that crosses it. Skia's `getIntercepts` finds those crossings — a
 * Western descender does, the CJK face (which sits above the line) does not.
 */
class SkipInkTest {

    private val engine = ExplainableStubParagraphLayoutEngine(
        lineBreaker = LookaheadLineBreaker(),
        textShaper = SkiaTextShaper(),
        fontMetricsResolver = SkiaFontMetricsResolver(),
    )
    private val size = 40f
    private val cjkFont = Font(SkiaSystemTypefaces.cjk, size)
    private val latinFont = Font(SkiaSystemTypefaces.latin, size)
    private val shaper = Shaper.makeShaperDrivenWrapper()

    private fun properNounSkips(text: String): FloatArray {
        val result = engine.layout(
            LayoutInput(
                content = TiqianTextContent(text),
                textStyle = TextStyle(fontSize = size),
                constraints = LayoutConstraints(maxWidth = 9999f),
                decorations = listOf(DecorationSpan(TextRange(0, text.length), DecorationKind.ProperNoun)),
            ),
        )
        val seg = result.debug.decorationSegments.first { it.kind == "ProperNoun" }
        return result.lineInkSkipIntervals(
            result.lines[seg.lineIndex], cjkFont, latinFont, shaper, seg.top - 2f, seg.top + 2f,
        )
    }

    @Test
    fun westernDescenderBreaksTheUnderline() {
        assertTrue(properNounSkips("Page").isNotEmpty(), "the g descender must cross the 专名号 line")
    }

    @Test
    fun pureCjkUnderlineStaysContinuous() {
        assertTrue(properNounSkips("中文名").isEmpty(), "CJK face is above the line — nothing to skip")
    }
}
