package org.tiqian.layout

import android.graphics.Paint
import android.graphics.text.LineBreaker
import android.graphics.text.MeasuredText
import android.text.TextPaint
import org.tiqian.linebreak.Hyphenator
import org.tiqian.linebreak.LineWidthHyphenator
import java.util.Collections
import java.util.LinkedHashMap
import java.util.Locale

/**
 * Android exposes paragraph-level hyphenation controls through StaticLayout /
 * LineBreaker, not a public word-level dictionary API. Use LineBreaker as a
 * platform oracle: probe a word at several realistic widths and collect the
 * intra-word offsets where Android actually chooses an end-hyphen edit.
 *
 * This is intentionally platform-derived, not golden-stable TeX behaviour.
 */
internal actual fun defaultHyphenator(): Hyphenator = AndroidLineBreakerHyphenator()

/**
 * Platform hyphenation probe backed by Android's public [LineBreaker].
 *
 * It does NOT try to enumerate every dictionary opportunity. Tiqian only needs
 * a set of plausible syllable breaks for mixed Western text; the line breaker
 * will still score and choose among them later. Results are cached per word.
 */
class AndroidLineBreakerHyphenator(
    private val locale: Locale = Locale.US,
    private val textPaintFactory: () -> TextPaint = {
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textLocale = Locale.US
            textSize = 16f
        }
    },
    private val minPrefix: Int = 2,
    private val minSuffix: Int = 3,
) : LineWidthHyphenator {

    private val cache = Collections.synchronizedMap(
        object : LinkedHashMap<String, List<Int>>(128, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<Int>>): Boolean =
                size > MaxCachedWords
        },
    )

    override fun hyphenate(word: String): List<Int> {
        if (word.length < minPrefix + minSuffix) return emptyList()
        if (!word.all { it in 'A'..'Z' || it in 'a'..'z' }) return emptyList()
        cache[word]?.let { return it }

        val paint = try {
            textPaintFactory().apply { textLocale = locale }
        } catch (e: RuntimeException) {
            if (e.isAndroidSdkStub()) return emptyList()
            throw e
        }
        val breaks = computeHyphenBreaks(word, paint, probeWidths(word, paint))

        return breaks.toList().also { cache[word] = it }
    }

    override fun hyphenateAtWidth(
        word: String,
        availableWidth: Float,
        measuredWordWidth: Float,
    ): Int? {
        if (word.length < minPrefix + minSuffix) return null
        if (!word.all { it in 'A'..'Z' || it in 'a'..'z' }) return null
        if (availableWidth <= 0f || measuredWordWidth <= 0f) return null

        val paint = try {
            textPaintFactory().apply { textLocale = locale }
        } catch (e: RuntimeException) {
            if (e.isAndroidSdkStub()) return null
            throw e
        }
        val platformWordWidth = paint.measureText(word).coerceAtLeast(1f)
        val platformWidth = availableWidth * (platformWordWidth / measuredWordWidth)
        return computeHyphenBreaks(word, paint, listOf(platformWidth)).singleOrNull()
    }

    private fun computeHyphenBreaks(
        word: String,
        paint: TextPaint,
        widths: List<Float>,
    ): Set<Int> {
        val measured = MeasuredText.Builder(word.toCharArray())
            .appendStyleRun(paint, word.length, false)
            .build()
        val breaker = LineBreaker.Builder()
            .setBreakStrategy(LineBreaker.BREAK_STRATEGY_HIGH_QUALITY)
            .setHyphenationFrequency(LineBreaker.HYPHENATION_FREQUENCY_FULL)
            .build()
        val constraints = LineBreaker.ParagraphConstraints()
        val breaks = linkedSetOf<Int>()

        for (width in widths) {
            constraints.setWidth(width)
            val result = breaker.computeLineBreaks(measured, constraints, 0)
            if (result.lineCount < 2) continue
            val offset = result.getLineBreakOffset(0)
            if (offset in minPrefix..(word.length - minSuffix) &&
                result.getEndLineHyphenEdit(0) != Paint.END_HYPHEN_EDIT_NO_EDIT
            ) {
                breaks += offset
            }
        }
        return breaks
    }

    private fun probeWidths(word: String, paint: TextPaint): List<Float> {
        val hyphen = paint.measureText("-").coerceAtLeast(1f)
        val widths = ArrayList<Float>()
        for (offset in minPrefix..(word.length - minSuffix)) {
            val prefix = paint.measureText(word, 0, offset)
            // Around the visible prefix + hyphen width: enough to make the
            // platform prefer a hyphenated break when one exists, while still
            // staying near the layout situations Tiqian will encounter.
            widths += (prefix + hyphen * 0.50f).coerceAtLeast(1f)
            widths += (prefix + hyphen * 0.95f).coerceAtLeast(1f)
        }
        return widths.distinct()
    }

    private companion object {
        const val MaxCachedWords = 512
    }
}

private fun RuntimeException.isAndroidSdkStub(): Boolean =
    message?.contains("not mocked") == true
