package com.storyteller_f.fuluent.rich_text_edit

import android.content.Context
import android.graphics.Color
import android.text.Layout
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * toggle 样式应用/移除。预期按当前实现的真实行为（黑盒）锁定。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ToggleTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    // ---- 字符型样式 ----

    /** 迁移自 ExampleInstrumentedTest.testApplyTextStyle */
    @Test
    fun applyThenToggleOffBold() {
        val editable = builder { append("hello") }
        val intRange = 0..4
        editable.toggle(intRange, BoldStyle::class.java)
        run {
            val spans = editable.getSpans(intRange, BoldStyle::class.java)
            assertEquals(1, spans.size)
            assertEquals(intRange, editable.getSpanRange(spans.first()))
        }
        editable.toggle(intRange, BoldStyle::class.java)
        assertTrue(editable.getSpans(intRange, BoldStyle::class.java).isEmpty())
    }

    /** 迁移自 ExampleInstrumentedTest.testApplyTextStyleWhenTwoExists */
    @Test
    fun mergeTwoAdjacentSameStyle() {
        val editable = builder { append("hello") }
        editable.toggle(0..1, BoldStyle::class.java)
        editable.toggle(2..3, BoldStyle::class.java)
        editable.toggle(0..3, BoldStyle::class.java)
        val styles = editable.getSpans(0..3, BoldStyle::class.java)
        assertEquals(1, styles.size)
        assertEquals(0..3, editable.getSpanRange(styles.first()))
    }

    /** T-char-4：在完整覆盖选区的样式中间再 toggle —— 记录当前真实行为。 */
    @Test
    fun toggleMiddleOfCoveringStyle() {
        val editable = builder { append("hello") }
        editable.toggle(0..5, BoldStyle::class.java)
        editable.toggle(1..3, BoldStyle::class.java)
        val spans = editable.getSpans(0..5, BoldStyle::class.java)
        assertEquals(1, spans.size)
        assertEquals(3..5, editable.getSpanRange(spans.first()))
    }

    /** T-char-5：选区左侧与已有样式重叠 —— 合并补齐为更大范围。 */
    @Test
    fun toggleLeftPartialOverlapExtends() {
        val editable = builder { append("hello") }
        editable.toggle(0..2, BoldStyle::class.java)
        editable.toggle(1..4, BoldStyle::class.java)
        val spans = editable.getSpans(0..4, BoldStyle::class.java)
        assertEquals(1, spans.size)
        assertEquals(0..4, editable.getSpanRange(spans.first()))
    }

    /** T-char-6：选区内部的小样式被吸收成覆盖更大范围的单一样式。 */
    @Test
    fun toggleAbsorbsInnerStyle() {
        val editable = builder { append("hello") }
        editable.toggle(1..2, BoldStyle::class.java)
        editable.toggle(0..4, BoldStyle::class.java)
        val spans = editable.getSpans(0..4, BoldStyle::class.java)
        assertEquals(1, spans.size)
        assertEquals(0..4, editable.getSpanRange(spans.first()))
    }

    // ---- 多值样式（ColorStyle）----

    /** 迁移自 ExampleInstrumentedTest.testApplyTextColor */
    @Test
    fun applyColorIsIdempotentForSameValue() {
        val editable = builder { append("hello") }
        editable.toggle(0..2, ColorStyle::class.java, ColorStyle(Color.RED))
        assertEquals(1, editable.detectStyle(0..2).size)
        editable.toggle(0..2, ColorStyle::class.java, ColorStyle(Color.RED))
        assertEquals(1, editable.detectStyle(0..2).size)
    }

    /** T-mv-3：不同颜色值替换旧值。 */
    @Test
    fun applyColorDifferentValueReplaces() {
        val editable = builder { append("hello") }
        editable.toggle(0..2, ColorStyle::class.java, ColorStyle(Color.RED))
        editable.toggle(0..2, ColorStyle::class.java, ColorStyle(Color.BLUE))
        val spans = editable.getSpans(0..2, ColorStyle::class.java)
        assertEquals(1, spans.size)
        assertEquals(Color.BLUE, spans.first().value)
    }

    /** T-mv-4：传 null 清除样式（clear() 内部即如此）。 */
    @Test
    fun clearRemovesColor() {
        val editable = builder { append("hello") }
        editable.toggle(0..2, ColorStyle::class.java, ColorStyle(Color.RED))
        editable.toggle(0..2, ColorStyle::class.java, null)
        assertTrue(editable.getSpans(0..2, ColorStyle::class.java).isEmpty())
    }

    // ---- 段落型样式 ----

    /** 迁移自 ExampleInstrumentedTest.testApplyHeadlineColor */
    @Test
    fun applyHeadlineThenChangeValue() {
        val editable = builder { append("hello") }
        editable.toggle(0..2, HeadlineStyle::class.java, HeadlineStyle(1, 2f, context))
        assertEquals(1, editable.detectStyle(0..2).size)
        editable.toggle(0..2, HeadlineStyle::class.java, HeadlineStyle(2, 1.3f, context))
        assertEquals(1, editable.detectStyle(0..2).size)
    }

    /** T-para-1：段落型样式覆盖整段。 */
    @Test
    fun headlineCoversWholeParagraph() {
        val editable = builder { append("hello") }
        editable.toggle(0..2, HeadlineStyle::class.java, HeadlineStyle(1, 2f, context))
        val spans = editable.getSpans(0..5, HeadlineStyle::class.java)
        assertEquals(1, spans.size)
        assertEquals(0..5, editable.getSpanRange(spans.first()))
    }

    /** T-para-3：同值再次切换移除。 */
    @Test
    fun headlineSameValueTogglesOff() {
        val editable = builder { append("hello") }
        editable.toggle(0..2, HeadlineStyle::class.java, HeadlineStyle(1, 2f, context))
        editable.toggle(0..2, HeadlineStyle::class.java, HeadlineStyle(1, 2f, context))
        assertTrue(editable.getSpans(0..5, HeadlineStyle::class.java).isEmpty())
    }

    /** T-para-4：对齐样式应用到段落。 */
    @Test
    fun applyAlignment() {
        val editable = builder { append("hello") }
        editable.toggle(
            0..2,
            AlignmentStyle::class.java,
            AlignmentStyle(Layout.Alignment.ALIGN_OPPOSITE)
        )
        val spans = editable.getSpans(0..5, AlignmentStyle::class.java)
        assertEquals(1, spans.size)
        assertEquals(Layout.Alignment.ALIGN_OPPOSITE, spans.first().alignment)
    }

    /** T-para-5：多段落时仅作用于光标所在段。 */
    @Test
    fun headlineOnlyAffectsTargetParagraph() {
        val editable = builder { append("aaa\nbbb") }
        // 第二段（索引 4..7），toggle 的段落由 selectionRange.first 决定
        editable.toggle(5..6, HeadlineStyle::class.java, HeadlineStyle(1, 2f, context))
        assertTrue(editable.getSpans(0..3, HeadlineStyle::class.java).isEmpty())
        assertEquals(1, editable.getSpans(4..7, HeadlineStyle::class.java).size)
    }

    // ---- 冲突（黑盒：当前 Headline 走 toggleParagraph，不强制 conflict）----

    /** T-conf-1：HeadlineStyle 声明与 Bold 冲突，但段落型样式不会真正移除 Bold。 */
    @Test
    fun headlineDoesNotEnforceConflictWithBold() {
        val editable = builder { append("hello") }
        editable.toggle(0..2, BoldStyle::class.java)
        editable.toggle(0..2, HeadlineStyle::class.java, HeadlineStyle(1, 2f, context))
        assertEquals(1, editable.getSpans(0..2, BoldStyle::class.java).size)
        assertEquals(1, editable.getSpans(0..5, HeadlineStyle::class.java).size)
    }
}
