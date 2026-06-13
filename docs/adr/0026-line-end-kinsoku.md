# ADR 0026: 行尾禁则——开括号/分隔号不得居行尾

- Status: Accepted
- Date: 2026-06-13

## Context

整体 review 发现：`KinsokuRule` 接口有 `forbiddenAtLineStart` 与
`forbiddenAtLineEnd` 两个方法，`ClreqPunctuationPolicies` 也按四档算出
行尾禁则，但 **breaker 全程只消费 `forbiddenAtLineStart`**——行尾禁则
计算了却从未生效。后果：

- 「开引号/开括号不得居行尾」（CLREQ 基本处理档的规则）未实现，行尾
  可能挂一个无依无靠的 `（` / `“`；
- GB 法 / 严格处理追加的「分隔号不得居行尾」是空操作——它依赖的机制
  不存在（这也使 ADR 0025 的 `>24→GB` 升档此前完全无效）。

无任何 ADR 把行尾禁则记为暂缓——是实现遗漏。

## Decision

行尾违禁标点用**断点回退**处理，而非 line-start 那套挤进/推出：

- 当贪心断点会让一行**以**行尾违禁标点（开引号括号；GB·严格 的分隔号）
  **结尾**时，断点回退到该标点之前——标点移到下一行行首（开括号在行首
  合法）。`adjustBreakForLineEnd`，连续多个一并回退，绝不把行清空
  （仅一个违禁标点独占整行的极端情形保留违规，避免死循环）。
- **只缩短当前行、不增长任何行 → 无溢出级联**。这是相对 line-start
  「推出」的镜像，但因为方向是向后移、不回拉，不需要 PushIn/shrink
  兜底，实现简单且封闭。
- 记为 `RepairOption.CarryNext`（penalty 0，强制且无代价），dump 显示
  `repair=CarryNext(ForbiddenAtLineEnd …)`，offender = 被移走的标点。
- 末行（段尾）不回退——无下一行可承接，段落以 `（` 结尾是文本问题。
- 禁则集 `forbiddenLineEndClusters` 由 engine 从解析出的 `KinsokuLevel`
  算出（与 `forbiddenLineStartClusters` 并列），随 profile 档变化。

greedy 与 lookahead 两条路径都在**提交断点时**回退并标注，保证 dump
一致（lookahead 不在评分阶段回退——评分是启发式，正确性由提交时回退
保证）。

## Consequences

- 「开括号不得居行尾」（基本处理）真正生效；GB 法的「分隔号不得居行尾」
  从空操作变为实操作——ADR 0025 的 `>24→GB` 升档现在名副其实。
- `line-end-kinsoku` fixture + golden（`CarryNext` 决策行）；breaker 单测
  覆盖回退与「独占整行不回退」边界；engine 单测验证从档解析禁则集。
- `CarryNext` 的 repair 标注在后续 line-start 修复（PushIn/Carry 复用
  同一行）时可能被覆盖——几何已定，标注以更晚者为准，可接受。
- 行尾禁则不参与 lookahead 评分，理论上偶有非最优断点；与既有
  line-start 推出同属启发式，未观测到问题。
