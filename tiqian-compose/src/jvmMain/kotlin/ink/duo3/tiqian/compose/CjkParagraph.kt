package ink.duo3.tiqian.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.Layout
import ink.duo3.tiqian.core.LayoutConstraints
import ink.duo3.tiqian.core.ParagraphStyle
import ink.duo3.tiqian.core.TextStyle
import ink.duo3.tiqian.layout.ExplainableStubParagraphLayoutEngine
import ink.duo3.tiqian.layout.LookaheadLineBreaker
import ink.duo3.tiqian.shaping.skia.SkiaTextShaper
import kotlin.math.ceil

/**
 * Renders [text] as a CJK paragraph with the Tiqian engine (Slice 7, ADR 0017).
 *
 * The composable makes NO layout decisions: measure runs the injected
 * [measurer] against the incoming width constraint and reports
 * `LayoutResult.size`; draw walks the result and paints language-tagged
 * Skia TextBlobs — the same glyph forms the engine measured.
 *
 * Engine units are pixels; map density at the [textStyle] boundary
 * (e.g. `fontSize = 16f * density`) until DPI handling lands.
 */
@Composable
fun CjkParagraph(
    text: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle(),
    paragraphStyle: ParagraphStyle = ParagraphStyle(),
    decorations: List<ink.duo3.tiqian.core.DecorationSpan> = emptyList(),
    measurer: ParagraphMeasurer = rememberParagraphMeasurer(),
) {
    val result = remember { mutableStateOf<ink.duo3.tiqian.core.LayoutResult?>(null) }
    Layout(
        modifier = modifier.drawBehind {
            result.value?.let { drawParagraph(it) }
        },
        content = {},
    ) { _, constraints ->
        val maxWidth = if (constraints.hasBoundedWidth) {
            constraints.maxWidth.toFloat()
        } else {
            DEFAULT_UNBOUNDED_WIDTH
        }
        val laidOut = measurer.measure(
            text = text,
            constraints = LayoutConstraints(maxWidth = maxWidth),
            textStyle = textStyle,
            paragraphStyle = paragraphStyle,
            decorations = decorations,
        )
        result.value = laidOut
        layout(
            width = ceil(laidOut.size.width).toInt().coerceIn(constraints.minWidth, constraints.maxWidth),
            height = ceil(laidOut.size.height).toInt().coerceAtLeast(constraints.minHeight),
        ) {}
    }
}

/** Default measurer: Skia shaper (real advances + halt/locl) + lookahead breaker. */
@Composable
fun rememberParagraphMeasurer(): ParagraphMeasurer =
    remember {
        ParagraphMeasurer(
            ExplainableStubParagraphLayoutEngine(
                lineBreaker = LookaheadLineBreaker(),
                textShaper = SkiaTextShaper(),
            ),
        )
    }

private const val DEFAULT_UNBOUNDED_WIDTH = 65_536f
