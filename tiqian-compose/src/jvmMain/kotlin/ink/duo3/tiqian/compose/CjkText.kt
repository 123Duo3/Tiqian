package ink.duo3.tiqian.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import ink.duo3.tiqian.clreq.ClreqProfile
import ink.duo3.tiqian.core.ParagraphStyle
import ink.duo3.tiqian.core.TextStyle

/** Mirrors the engine's default body line height (1.5em); used to size 节 gaps. */
private const val BODY_LINE_HEIGHT_EM = 1.5f

/**
 * Lays out MULTIPLE paragraphs and sections (CLREQ §6.2.1 段落调整). Each
 * paragraph runs the engine via [CjkParagraph]; `CjkText` only splits the
 * source into blocks, maps each block's 段首缩排 style to the engine's
 * `blockIndentEm`/`firstLineIndentEm`, and stacks them.
 *
 * **行距段内段间一致**（CLREQ:「前段落末行、后段落首行与段落内行距一致」）成立于
 * 零 `Column` 间距：每个行盒上下各半 leading，相邻两段贴合后跨段 baseline 间距恰好
 * 一个 `lineHeight`。段间留白只来自显式的 [CjkBlock.Section]（空行=节）。
 */
@Composable
fun CjkText(
    blocks: List<CjkBlock>,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle(),
    paragraphStyle: ParagraphStyle = ParagraphStyle(),
    profile: ClreqProfile = ClreqProfile.MainlandHorizontal,
    measurer: ParagraphMeasurer = rememberParagraphMeasurer(profile),
) {
    // 节 (Section) = one blank line of vertical space.
    val sectionPx = paragraphStyle.lineHeight ?: (textStyle.fontSize * BODY_LINE_HEIGHT_EM)
    val sectionDp = with(LocalDensity.current) { sectionPx.toDp() }
    Column(modifier) {
        blocks.forEach { block ->
            when (block) {
                is CjkBlock.Section -> Spacer(Modifier.height(sectionDp))
                is CjkBlock.Paragraph -> {
                    val (blockEm, firstEm) = block.indent.resolve(paragraphStyle.firstLineIndentEm)
                    CjkParagraph(
                        text = block.text,
                        textStyle = textStyle,
                        paragraphStyle = paragraphStyle.copy(blockIndentEm = blockEm, firstLineIndentEm = firstEm),
                        profile = profile,
                        measurer = measurer,
                    )
                }
            }
        }
    }
}

/**
 * Convenience entry: `\n` separates paragraphs, a blank line becomes a 节
 * ([CjkBlock.Section]). [leadStyle] assigns the per-paragraph 段首缩排 style.
 * For mixed documents (dialogue 凸排, quote 段落缩排) build [CjkBlock]s directly.
 */
@Composable
fun CjkText(
    text: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle(),
    paragraphStyle: ParagraphStyle = ParagraphStyle(),
    profile: ClreqProfile = ClreqProfile.MainlandHorizontal,
    leadStyle: ParagraphLeadStyle = ParagraphLeadStyle.AllIndent,
    measurer: ParagraphMeasurer = rememberParagraphMeasurer(profile),
) {
    val blocks = remember(text, leadStyle) { parseBlocks(text, leadStyle) }
    CjkText(blocks, modifier, textStyle, paragraphStyle, profile, measurer)
}

/** A block in a CJK document (CLREQ §6.2.1). */
sealed interface CjkBlock {
    data class Paragraph(
        val text: String,
        val indent: ParagraphIndent = ParagraphIndent.FirstLine,
    ) : CjkBlock

    /** 空行 = 一节的结束（CLREQ）：renders as one blank line of space. */
    data object Section : CjkBlock
}

/**
 * Per-paragraph 段首缩排 style (CLREQ §6.2.1.1/§6.2.1.2). Resolves to the
 * engine's `(blockIndentEm, firstLineIndentEm)`; the indent AMOUNT for
 * [FirstLine] stays the engine's MeasureAdaptiveFirstLineIndent decision.
 */
sealed interface ParagraphIndent {
    /** 段首缩进（自适应/基准）。 */
    data object FirstLine : ParagraphIndent

    /** 不缩进。 */
    data object Flush : ParagraphIndent

    /** 凸排：首行齐头、次行起缩 [em] 字（对话/列表/法条）。 */
    data class Hanging(val em: Float = 2f) : ParagraphIndent

    /** 段落缩排：整段缩 [em] 字（引用/诗词），首行再额外缩 [firstLineEm]。 */
    data class Block(val em: Float = 2f, val firstLineEm: Float = 0f) : ParagraphIndent

    /** → `(blockIndentEm, firstLineIndentEm)`; null firstLine = adaptive default. */
    fun resolve(base: Float?): Pair<Float, Float?> = when (this) {
        FirstLine -> 0f to base
        Flush -> 0f to 0f
        is Hanging -> em to -em
        is Block -> em to firstLineEm
    }
}

/** CLREQ §6.2.1.1 段首缩排 的跨段风格（用于 [CjkText] 的纯文本入口）。 */
enum class ParagraphLeadStyle {
    /** ① 全段首行缩进（书刊默认）。 */
    AllIndent,

    /** ② 首段不缩、其余段缩进（西文书习惯）。 */
    FirstParagraphFlush,

    /** ③ 全不缩进、段间以 节 留白区分。 */
    NoIndentSpaced,
}

/**
 * `\n` → 段落，空行 → 节。前导/尾随空行与连续空行折叠为单个 节。NoIndentSpaced
 * 在相邻段落间插入 节（无缩进时靠留白区分段）。
 */
private fun parseBlocks(text: String, leadStyle: ParagraphLeadStyle): List<CjkBlock> {
    val lines = text.split('\n').map { it.trim('\r') }
    val blocks = mutableListOf<CjkBlock>()
    var paragraphIndex = 0
    var pendingSection = false
    for (line in lines) {
        if (line.isBlank()) {
            if (blocks.isNotEmpty()) pendingSection = true
            continue
        }
        if (pendingSection) {
            blocks += CjkBlock.Section
            pendingSection = false
        } else if (leadStyle == ParagraphLeadStyle.NoIndentSpaced && paragraphIndex > 0) {
            blocks += CjkBlock.Section
        }
        val indent = when (leadStyle) {
            ParagraphLeadStyle.AllIndent -> ParagraphIndent.FirstLine
            ParagraphLeadStyle.FirstParagraphFlush ->
                if (paragraphIndex == 0) ParagraphIndent.Flush else ParagraphIndent.FirstLine
            ParagraphLeadStyle.NoIndentSpaced -> ParagraphIndent.Flush
        }
        blocks += CjkBlock.Paragraph(line, indent)
        paragraphIndex++
    }
    return blocks
}
