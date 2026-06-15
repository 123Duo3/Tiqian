package ink.duo3.tiqian.compose

import ink.duo3.tiqian.core.DecorationSpan
import ink.duo3.tiqian.core.LayoutConstraints
import ink.duo3.tiqian.core.LayoutInput
import ink.duo3.tiqian.core.LayoutResult
import ink.duo3.tiqian.core.ParagraphStyle
import ink.duo3.tiqian.core.TextStyle
import ink.duo3.tiqian.core.TiqianTextContent
import ink.duo3.tiqian.layout.ExplainableStubParagraphLayoutEngine
import ink.duo3.tiqian.layout.ParagraphLayoutEngine

/**
 * Lays out a CJK paragraph with the Tiqian engine and returns the measured
 * [LayoutResult]. A thin Compose-side wrapper over [ParagraphLayoutEngine] —
 * it makes no layout decisions of its own.
 */
class ParagraphMeasurer(
    private val engine: ParagraphLayoutEngine = ExplainableStubParagraphLayoutEngine(),
) {
    fun measure(
        text: String,
        constraints: LayoutConstraints,
        textStyle: TextStyle = TextStyle(),
        paragraphStyle: ParagraphStyle = ParagraphStyle(),
        decorations: List<DecorationSpan> = emptyList(),
    ): LayoutResult =
        engine.layout(
            LayoutInput(
                content = TiqianTextContent(text),
                textStyle = textStyle,
                paragraphStyle = paragraphStyle,
                constraints = constraints,
                decorations = decorations,
            ),
        )
}
