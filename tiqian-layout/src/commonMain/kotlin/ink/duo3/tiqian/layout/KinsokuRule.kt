package ink.duo3.tiqian.layout

import ink.duo3.tiqian.clreq.ClreqPunctuationPolicies
import ink.duo3.tiqian.clreq.KinsokuLevel
import ink.duo3.tiqian.core.Cluster

/**
 * KinsokuRule — line-start / line-end forbiddance for CJK punctuation.
 *
 * Default [ClreqKinsokuRule] reads from [ClreqPunctuationPolicies] at the
 * given CLREQ [KinsokuLevel] (default [KinsokuLevel.Basic], 最推荐).
 * Profile-specific overrides should construct with another level or replace
 * this rule, not edit the engine.
 */
interface KinsokuRule {
    fun forbiddenAtLineStart(cluster: Cluster): Boolean
    fun forbiddenAtLineEnd(cluster: Cluster): Boolean
}

class ClreqKinsokuRule(
    private val level: KinsokuLevel = KinsokuLevel.Basic,
) : KinsokuRule {
    override fun forbiddenAtLineStart(cluster: Cluster): Boolean {
        val char = cluster.displayText.firstOrNull() ?: return false
        return ClreqPunctuationPolicies.forbiddenAtLineStart(char, level)
    }

    override fun forbiddenAtLineEnd(cluster: Cluster): Boolean {
        val char = cluster.displayText.firstOrNull() ?: return false
        return ClreqPunctuationPolicies.forbiddenAtLineEnd(char, level)
    }
}
