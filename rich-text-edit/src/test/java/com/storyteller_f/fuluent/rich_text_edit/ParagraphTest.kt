package com.storyteller_f.fuluent.rich_text_edit

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * CharSequence.paragraphAt 的纯逻辑测试。无需 Android 环境。
 */
class ParagraphTest {

    @Test
    fun firstParagraphMiddle() =
        assertEquals(Paragraph(0, 4), "test\nhello".paragraphAt(1))

    @Test
    fun secondParagraphMiddle() =
        assertEquals(Paragraph(5, 10), "test\nhello".paragraphAt(6))

    @Test
    fun cursorAtStart() =
        assertEquals(Paragraph(0, 3), "abc".paragraphAt(0))

    @Test
    fun cursorAtEnd() =
        assertEquals(Paragraph(0, 3), "abc".paragraphAt(3))

    @Test
    fun singleParagraphNoNewline() =
        assertEquals(Paragraph(0, 5), "hello".paragraphAt(2))

    @Test
    fun emptyString() =
        assertEquals(Paragraph(0, 0), "".paragraphAt(0))

    @Test
    fun cursorOnNewline() =
        assertEquals(Paragraph(0, 1), "a\nb".paragraphAt(1))

    @Test
    fun cursorAtParagraphStartAfterNewline() =
        assertEquals(Paragraph(2, 3), "a\nb".paragraphAt(2))

    @Test
    fun emptyParagraphBetweenNewlines() =
        assertEquals(Paragraph(2, 2), "a\n\nb".paragraphAt(2))
}
