package ink.duo3.tiqian.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.singleWindowApplication
import ink.duo3.tiqian.core.DecorationKind
import ink.duo3.tiqian.core.DecorationSpan
import ink.duo3.tiqian.core.ParagraphStyle
import ink.duo3.tiqian.core.TextRange
import ink.duo3.tiqian.core.TextStyle

private const val PARAGRAPH =
    "《原神》是由米哈游自研的一款开放世界冒险RPG。你将在游戏中探索一个被称作「提瓦特」的幻想世界。在这广阔的世界中，你可以踏遍七国，邂逅性格各异、能力独特的同伴，与他们一同对抗强敌，踏上寻回血亲之路；也可以不带目的地漫游，沉浸在充满生机的世界里，让好奇心驱使自己发掘各个角落的奥秘……直到你与分离的血亲重聚，在终点见证一切事物的沉淀。"

fun main() = singleWindowApplication(title = "Tiqian Compose Demo") {
    // Engine units are physical pixels; map dp at the TextStyle boundary
    // (ADR 0017) so the demo reads at 15dp on any density.
    val fontSizePx = with(LocalDensity.current) { 15.dp.toPx() }
    val textStyle = TextStyle(fontSize = fontSizePx)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        var text by remember { mutableStateOf("") }

        TextField(
            value = text,
            onValueChange = { text = it },
        )
        CjkParagraph(
            text = text,
            modifier = Modifier.width(480.dp),
            textStyle = textStyle,
        )
        CjkParagraph(
            text = PARAGRAPH,
            modifier = Modifier.width(480.dp),
            textStyle = textStyle,
            paragraphStyle = ParagraphStyle(lineHeight = 25F)
        )
        CjkParagraph(
            text = "他说：“你好，世界。”中文……English——中文。",
            modifier = Modifier.width(480.dp),
            textStyle = textStyle,
        )
        CjkParagraph(
            text = "他强调：豆子新鲜最要紧，烘焙其次。",
            modifier = Modifier.width(480.dp),
            textStyle = textStyle,
            decorations = listOf(
                DecorationSpan(
                    range = TextRange(4, 16),
                    kind = DecorationKind.Emphasis,
                ),
            ),
        )
    }
}
