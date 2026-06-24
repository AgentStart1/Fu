package com.storyteller_f.fuluent.rich_text_edit

import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * detectStyle / detectCoveredStyle：检测区间内完整覆盖的样式。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DetectStyleTest {

    /** D-1：完整覆盖被检出。 */
    @Test
    fun fullyCoveredDetected() {
        val editable = builder { append("hello") }
        editable.toggle(0..2, ColorStyle::class.java, ColorStyle(Color.RED))
        assertEquals(1, editable.detectStyle(0..2).size)
    }

    /** D-2：尾字符是标点 -> 光标处不继承（迁移自 testAutoApplyTextStyle）。 */
    @Test
    fun tailPunctuationNotInherited() {
        val editable = builder { append("hello!") }
        editable.toggle(0..6, BoldStyle::class.java)
        assertEquals(0, editable.detectStyle(6..6).size)
    }

    /** D-3：尾字符非标点 -> 继承。 */
    @Test
    fun tailNonPunctuationInherited() {
        val editable = builder { append("hello") }
        editable.toggle(0..5, BoldStyle::class.java)
        assertEquals(1, editable.detectStyle(5..5).size)
    }

    /** D-4：仅部分覆盖不检出。 */
    @Test
    fun partialCoverageNotDetected() {
        val editable = builder { append("hello") }
        editable.toggle(0..2, BoldStyle::class.java)
        assertEquals(0, editable.detectStyle(0..4).size)
    }

    /** D-5：被 Break 打断的样式被排除。 */
    @Test
    fun brokenStyleExcluded() {
        val editable = builder { append("hello") }
        editable.toggle(0..5, BoldStyle::class.java)
        @Suppress("UNCHECKED_CAST")
        editable.setSpan(Break(BoldStyle::class.java as Class<RichSpan>), 0, 5, 0)
        assertEquals(0, editable.detectStyle(0..5).size)
    }

    /** D-6：同区间多样式同时检出。 */
    @Test
    fun multipleStylesDetected() {
        val editable = builder { append("hello") }
        editable.toggle(0..2, BoldStyle::class.java)
        editable.toggle(0..2, ColorStyle::class.java, ColorStyle(Color.RED))
        assertEquals(2, editable.detectStyle(0..2).size)
    }

    /** D-7：无样式时为空。 */
    @Test
    fun noStyleEmpty() {
        val editable = builder { append("hello") }
        assertEquals(0, editable.detectStyle(0..0).size)
    }
}
