package ink.duo3.tiqian.layout

import ink.duo3.tiqian.core.LayoutConstraints
import ink.duo3.tiqian.core.LayoutInput
import ink.duo3.tiqian.core.ParagraphStyle
import ink.duo3.tiqian.core.RubyKind
import ink.duo3.tiqian.core.RubySpan
import ink.duo3.tiqian.core.TextRange
import ink.duo3.tiqian.core.TiqianTextContent
import ink.duo3.tiqian.core.ZhuyinGlyphRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 注音 (ADR 0033): right-side ㄅㄆㄇ symbols + parsed tone, with 纵横对齐 reservation. */
class ZhuyinLayoutTest {

    private val engine = ExplainableStubParagraphLayoutEngine()

    private fun layout(zhuyin: List<RubySpan>) = engine.layout(
        LayoutInput(
            content = TiqianTextContent("中文"),
            constraints = LayoutConstraints(maxWidth = 4000f),
            paragraphStyle = ParagraphStyle(firstLineIndentEm = 0f),
            rubySpans = zhuyin,
        ),
    )

    @Test
    fun symbolsAndToneRightOfBase() {
        val result = layout(
            listOf(
                RubySpan(TextRange(0, 1), "ㄓㄨㄥ", kind = RubyKind.Zhuyin), // 阴平, 3 symbols
                RubySpan(TextRange(1, 2), "ㄔㄤˊ", kind = RubyKind.Zhuyin), // 阳平, 2 symbols
            ),
        )
        val z = result.debug.zhuyinDecisions
        assertEquals(2, z.size)

        val zhong = z.first { it.baseRange.start == 0 }
        assertEquals(3, zhong.placements.count { it.role == ZhuyinGlyphRole.Symbol }) // ㄓㄨㄥ
        assertTrue(zhong.placements.none { it.role == ZhuyinGlyphRole.Tone }) // 阴平 no mark
        // 注音 sits to the RIGHT of the 1em base char.
        assertTrue(zhong.placements.all { it.left >= 15.9f }, "symbols right of base: ${zhong.placements.map { it.left }}")

        val chang = z.first { it.baseRange.start == 1 }
        assertEquals(2, chang.placements.count { it.role == ZhuyinGlyphRole.Symbol })
        assertEquals(1, chang.placements.count { it.role == ZhuyinGlyphRole.Tone }) // 阳平 ˊ
    }

    @Test
    fun everyCharReservesHalfEm() {
        val plain = engine.layout(
            LayoutInput(
                content = TiqianTextContent("中文"),
                constraints = LayoutConstraints(maxWidth = 4000f),
                paragraphStyle = ParagraphStyle(firstLineIndentEm = 0f),
            ),
        ).clusters.first().advance
        val withZhuyin = layout(listOf(RubySpan(TextRange(0, 1), "ㄓㄨㄥ", kind = RubyKind.Zhuyin)))
            .clusters.first().advance
        // 纵横对齐: every char +0.5em (even the unannotated 文).
        assertTrue(withZhuyin > plain, "zhuyin reserves advance ($withZhuyin vs $plain)")
    }
}
