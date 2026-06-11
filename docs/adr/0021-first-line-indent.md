# ADR 0021: 段首缩进

- Status: Accepted
- Date: 2026-06-12

## Context

CLREQ：「段首缩排以两个汉字的空间为标准。若遇到杂志等多栏排版……时有改用
缩排一字」；「若首行行首出现开始夹注符号，可以缩减该符号始侧二分之一个
汉字大小的空白」。此前段落级特性完全缺失（gap audit 缺口 5）。

## Decision

### `ParagraphStyle.firstLineIndentEm`，默认 2

缩进是段落样式而非 CLREQ profile 规则——量纲用 em，跟随字号。默认取
CLREQ 标准两字宽：本引擎面向中文正文，默认输出即应是合格正文段落。
多栏/杂志风格配 1，置 0 关闭。手调几何的 fixture 与单测显式 pin 0
（缩进不是它们要验证的变量）。

### 缩进 = 首行可用行宽收窄 + 渲染起点偏移

- breaker 端：起始于 cluster 0 的行（含 lookahead 评分、PushIn 合并行）
  可用行宽为 `maxWidth - firstLineIndent`，其余行不变（`lineLimit`）。
- justify 端：首行的对齐目标同样是收窄后的行宽。
- 结果端：`LineBox.indent` 暴露行首 inline 轴起点偏移；宽度字段不含
  缩进，`LayoutResult.size.width` 取 `max(indent + visualWidth)`。
  渲染层（compose / playground raster / HTML）只做 `x = line.indent`
  起步，不参与任何决策。decoration（着重号锚点、示亡号框）几何在
  engine 端已含偏移。

### 首行开括号「缩减半宽」不需要实现

加法模型下开括号 = leading glue (0.5em 空白) + body；行首 leading glue
在任何行首都被 `consumeLineEdgeGlue` 削掉（ADR 0010），段落首行不例外。
缩进 2em 后紧跟已削前空白的开括号，字面前视觉空白恰为 2em——CLREQ 的
「可以缩减」是模型的自然推论（`indent-opening-quote` golden 印证）。
CLREQ 原文是「可以」；我们的行首 trim 无条件，取严格侧，与行末
`ForceHalfWidth` 默认一致。若将来需要「首行开括号保留全宽」的宽松
风格再加开关，现在不预留。

### 竖排时它该怎么改

`LineBox.indent` 语义是「inline 轴起点偏移」，竖排即首列顶端的
block-start inset，字段无需改；数值仍是 2em。

## Consequences

- `first-line-indent` / `indent-opening-quote` fixture + golden；
  `real-paragraph-1` 改为带标准缩进的真实正文形态。
- dump：line 行新增 `indent=` 字段（仅非零时输出）。
- compose demo 与 playground 默认呈现缩进段落。
- `ParagraphStyle` 既有默认值的不一致暴露出来：`textAlign` 默认 Start
  而中文正文应 Justify。是否把 Justify 设为默认留待单独讨论，本 ADR
  不顺手改。
