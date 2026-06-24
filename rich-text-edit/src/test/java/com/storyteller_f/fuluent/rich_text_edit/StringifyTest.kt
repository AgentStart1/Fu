package com.storyteller_f.fuluent.rich_text_edit

import android.content.Context
import android.graphics.Color
import android.text.Layout
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Stringify：richFormatPlain 序列化与 parseRichFormatPlain 解析。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StringifyTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    /** S-1 / S-3：单 Headline 序列化并解析回 span（迁移自 testCharSequenceStringify）。 */
    @Test
    fun headlineSerializeAndParse() {
        val editable = builder { append("hello") }
        editable.toggle(0..2, HeadlineStyle::class.java, HeadlineStyle(1, 2f, context))
        val json = editable.richFormatPlain()
        assertEquals("""[{"type":"headline","start":0,"end":5,"data":1}]""", json)
        val parsed = context.parseRichFormatPlain("hello", json)
        assertEquals(1, parsed.getSpans(0..2, Any::class.java).size)
    }

    /** S-2：Alignment 序列化（迁移自 testAlignmentCharSequenceStringify）。 */
    @Test
    fun alignmentSerialize() {
        val editable = builder { append("hello") }
        editable.toggle(
            0..2,
            AlignmentStyle::class.java,
            AlignmentStyle(Layout.Alignment.ALIGN_OPPOSITE)
        )
        assertEquals(
            """[{"type":"align","start":0,"end":5,"data":1}]""",
            editable.richFormatPlain()
        )
    }

    /** S-4：颜色样式的 data 字段携带颜色值。 */
    @Test
    fun colorSerializeCarriesValue() {
        val editable = builder { append("hello") }
        editable.toggle(0..2, ColorStyle::class.java, ColorStyle(Color.RED))
        assertEquals(
            """[{"type":"color","start":0,"end":2,"data":${Color.RED}}]""",
            editable.richFormatPlain()
        )
    }

    /** S-5：无样式序列化为空数组。 */
    @Test
    fun emptySerialize() {
        val editable = builder { append("hello") }
        assertEquals("[]", editable.richFormatPlain())
    }

    /** S-6：多样式序列化包含全部条目。 */
    @Test
    fun multipleStylesSerialize() {
        val editable = builder { append("hello") }
        editable.toggle(0..2, BoldStyle::class.java)
        editable.toggle(1..3, ColorStyle::class.java, ColorStyle(Color.RED))
        val json = editable.richFormatPlain()
        assertTrue(json, json.contains(""""type":"bold","start":0,"end":2,"data":0"""))
        assertTrue(json, json.contains(""""type":"color","start":1,"end":3,"data":${Color.RED}"""))
    }

    /** S-7：非多值样式 data 为 0。 */
    @Test
    fun nonMultiValueDataIsZero() {
        val editable = builder { append("hello") }
        editable.toggle(0..2, BoldStyle::class.java)
        assertEquals("""[{"type":"bold","start":0,"end":2,"data":0}]""", editable.richFormatPlain())
    }

    /** S-8：未知 type 解析抛异常。 */
    @Test
    fun unknownTypeThrows() {
        val json = """[{"type":"unknown","start":0,"end":2,"data":0}]"""
        assertThrows(Exception::class.java) {
            context.parseRichFormatPlain("hello", json)
        }
    }
}
