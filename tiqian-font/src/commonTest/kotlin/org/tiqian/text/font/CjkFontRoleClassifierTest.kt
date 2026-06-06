package org.tiqian.text.font

import org.tiqian.text.core.TextRange
import kotlin.test.Test
import kotlin.test.assertEquals

class CjkFontRoleClassifierTest {
    private val classifier = CjkFontRoleClassifier()

    @Test
    fun classifiesCjkText() {
        assertEquals(FontRole.CjkText, classifier.classify("提", TextRange(0, 1)))
    }

    @Test
    fun classifiesCjkPunctuation() {
        assertEquals(FontRole.CjkPunctuation, classifier.classify("……", TextRange(0, 1)))
        assertEquals(FontRole.CjkPunctuation, classifier.classify("⋯⋯", TextRange(0, 1)))
        assertEquals(FontRole.CjkPunctuation, classifier.classify("——", TextRange(0, 1)))
        assertEquals(FontRole.CjkPunctuation, classifier.classify("⸺", TextRange(0, 1)))
        assertEquals(FontRole.CjkPunctuation, classifier.classify("。", TextRange(0, 1)))
        assertEquals(FontRole.CjkPunctuation, classifier.classify("・", TextRange(0, 1)))
        assertEquals(FontRole.CjkPunctuation, classifier.classify("‧", TextRange(0, 1)))
        assertEquals(FontRole.CjkPunctuation, classifier.classify("～", TextRange(0, 1)))
        assertEquals(FontRole.CjkPunctuation, classifier.classify("-", TextRange(0, 1)))
        assertEquals(FontRole.CjkPunctuation, classifier.classify("/", TextRange(0, 1)))
        assertEquals(FontRole.CjkPunctuation, classifier.classify("／", TextRange(0, 1)))
    }

    @Test
    fun classifiesLatinText() {
        assertEquals(FontRole.LatinText, classifier.classify("English", TextRange(0, 1)))
    }

    @Test
    fun keepsAsciiPunctuationInsideLatinTechnicalRuns() {
        assertEquals(FontRole.LatinText, classifier.classify("well-known", TextRange(4, 5)))
        assertEquals(FontRole.LatinText, classifier.classify("https://example", TextRange(6, 7)))
        assertEquals(FontRole.LatinText, classifier.classify("https://example", TextRange(7, 8)))
        assertEquals(FontRole.CjkPunctuation, classifier.classify("中文/中文", TextRange(2, 3)))
    }
}
