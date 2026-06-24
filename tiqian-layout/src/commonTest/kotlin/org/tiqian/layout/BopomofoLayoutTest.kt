package org.tiqian.layout

import org.tiqian.core.Ic

import org.tiqian.core.LayoutConstraints
import org.tiqian.core.LayoutInput
import org.tiqian.core.ParagraphStyle
import org.tiqian.core.RubyKind
import org.tiqian.core.RubySpan
import org.tiqian.core.TextRange
import org.tiqian.core.TiqianTextContent
import org.tiqian.core.BopomofoGlyphRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 注音 (ADR 0033): right-side ㄅㄆㄇ symbols + parsed tone, with 纵横对齐 reservation. */
class BopomofoLayoutTest {

    private val engine = ExplainableStubParagraphLayoutEngine()

    private fun layout(bopomofo: List<RubySpan>) = engine.layout(
        LayoutInput(
            content = TiqianTextContent("中文"),
            constraints = LayoutConstraints(maxWidth = 4000f),
            paragraphStyle = ParagraphStyle(firstLineIndent = Ic(0f)),
            rubySpans = bopomofo,
        ),
    )

    @Test
    fun symbolsAndToneRightOfBase() {
        val result = layout(
            listOf(
                RubySpan(TextRange(0, 1), "ㄓㄨㄥ", kind = RubyKind.Bopomofo), // 阴平, 3 symbols
                RubySpan(TextRange(1, 2), "ㄔㄤˊ", kind = RubyKind.Bopomofo), // 阳平, 2 symbols
            ),
        )
        val z = result.debug.bopomofoDecisions
        assertEquals(2, z.size)

        val zhong = z.first { it.baseRange.start == 0 }
        assertEquals(700, zhong.fontWeight, "bopomofo defaults three weight steps heavier than base")
        assertEquals(3, zhong.placements.count { it.role == BopomofoGlyphRole.Symbol }) // ㄓㄨㄥ
        assertTrue(zhong.placements.none { it.role == BopomofoGlyphRole.Tone }) // 阴平 no mark
        // 注音 sits to the RIGHT of the 1em base char.
        assertTrue(zhong.placements.all { it.left >= 15.9f }, "symbols right of base: ${zhong.placements.map { it.left }}")

        val chang = z.first { it.baseRange.start == 1 }
        assertEquals(2, chang.placements.count { it.role == BopomofoGlyphRole.Symbol })
        assertEquals(1, chang.placements.count { it.role == BopomofoGlyphRole.Tone }) // 阳平 ˊ
    }

    @Test
    fun everyCharReservesHalfEm() {
        val plain = engine.layout(
            LayoutInput(
                content = TiqianTextContent("中文"),
                constraints = LayoutConstraints(maxWidth = 4000f),
                paragraphStyle = ParagraphStyle(firstLineIndent = Ic(0f)),
            ),
        ).clusters.first().advance
        val withBopomofo = layout(listOf(RubySpan(TextRange(0, 1), "ㄓㄨㄥ", kind = RubyKind.Bopomofo)))
            .clusters.first().advance
        // 纵横对齐: every char +0.5em (even the unannotated 文).
        assertTrue(withBopomofo > plain, "bopomofo reserves advance ($withBopomofo vs $plain)")
    }
}
