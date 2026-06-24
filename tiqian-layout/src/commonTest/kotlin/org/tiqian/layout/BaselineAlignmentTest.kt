package org.tiqian.layout

import org.tiqian.core.Ic
import org.tiqian.core.LayoutConstraints
import org.tiqian.core.LayoutInput
import org.tiqian.core.ParagraphStyle
import org.tiqian.core.TextRange
import org.tiqian.core.TextSpan
import org.tiqian.core.TextStyle
import org.tiqian.core.TiqianTextContent
import kotlin.test.Test
import kotlin.test.assertEquals

class BaselineAlignmentTest {

    @Test
    fun latinInsideCjkUsesSharedRomanBaseline() {
        val result = ExplainableStubParagraphLayoutEngine().layout(
            LayoutInput(
                content = TiqianTextContent("中A文"),
                constraints = LayoutConstraints(maxWidth = 400f),
                paragraphStyle = ParagraphStyle(firstLineIndent = Ic(0f)),
            ),
        )

        val latin = result.clusters.single { it.text == "A" }
        assertEquals(0f, latin.baselineShift, "Latin mixed into CJK should use the shared Roman baseline")
    }

    @Test
    fun cjkMixedSizesAlignByIdeographicBoxBottom() {
        val result = ExplainableStubParagraphLayoutEngine().layout(
            LayoutInput(
                content = TiqianTextContent(
                    text = "中小大",
                    spans = listOf(
                        TextSpan(TextRange(1, 2), TextStyle(fontSize = 12f)),
                        TextSpan(TextRange(2, 3), TextStyle(fontSize = 20f)),
                    ),
                ),
                constraints = LayoutConstraints(maxWidth = 400f),
                paragraphStyle = ParagraphStyle(firstLineIndent = Ic(0f)),
            ),
        )

        val base = result.clusters.single { it.text == "中" }
        val small = result.clusters.single { it.text == "小" }
        val large = result.clusters.single { it.text == "大" }
        assertEquals(0f, base.baselineShift)
        assertEquals(0.48f, small.baselineShift, 0.01f)
        assertEquals(-0.48f, large.baselineShift, 0.01f)
    }
}
