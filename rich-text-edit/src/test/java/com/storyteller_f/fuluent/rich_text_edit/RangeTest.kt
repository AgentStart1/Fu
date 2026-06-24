package com.storyteller_f.fuluent.rich_text_edit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Range.kt 中的纯区间运算。无需 Android 环境。
 */
class RangeTest {

    // cover: A 包含 B（含端点相等）
    @Test
    fun cover_contains() = assertTrue((0..10) cover (2..5))

    @Test
    fun cover_equalEndpoints() = assertTrue((0..10) cover (0..10))

    @Test
    fun cover_smallerCannotCoverLarger() = assertFalse((2..5) cover (0..10))

    @Test
    fun cover_partialOverlapIsNotCover() = assertFalse((0..5) cover (3..8))

    // coerce: 收窄到 range 范围内。
    // 注意：coerce 返回的是泛型 ClosedRange（ComparableRange），与 IntRange 不是同一类型，
    // 故按端点比较而非整体 equals。
    private fun assertRange(start: Int, end: Int, actual: ClosedRange<Int>) {
        assertEquals(start, actual.start)
        assertEquals(end, actual.endInclusive)
    }

    @Test
    fun coerce_clampInside() = assertRange(3, 7, (0..10) coerce (3..7))

    @Test
    fun coerce_clampOneSide() = assertRange(5, 10, (0..10) coerce (5..20))

    @Test
    fun coerce_fullyInsideUnchanged() = assertRange(3, 7, (3..7) coerce (0..10))

    // leftPartial: A 跨过 B 左边界，右端落在 B 内部
    @Test
    fun leftPartial_standard() = assertTrue((0..5) leftPartial (3..10))

    @Test
    fun leftPartial_touchingStartIsNot() = assertFalse((0..3) leftPartial (3..10))

    @Test
    fun leftPartial_fullCoverIsNot() = assertFalse((0..20) leftPartial (3..10))

    // rightPartial: A 右端越过 B 右边界
    @Test
    fun rightPartial_standard() = assertTrue((5..15) rightPartial (3..10))

    @Test
    fun rightPartial_touchingEndIsNot() = assertFalse((10..15) rightPartial (3..10))

    // partial = leftPartial || rightPartial
    @Test
    fun partial_leftOnly() = assertTrue((0..5) partial (3..10))

    @Test
    fun partial_rightOnly() = assertTrue((5..15) partial (3..10))

    // 全包含也算 rightPartial（rightPartial 只要求 A.start < B.end < A.end），故 partial 为 true。
    @Test
    fun partial_fullCoverIsRightPartial() = assertTrue((0..20) partial (3..10))

    @Test
    fun partial_disjointIsNot() = assertFalse((0..2) partial (3..10))

    // inner: A 严格落在 B 内部
    @Test
    fun inner_strictlyInside() = assertTrue((4..6) inner (3..10))

    @Test
    fun inner_touchingStartIsNot() = assertFalse((3..6) inner (3..10))

    @Test
    fun inner_equalIsNot() = assertFalse((3..10) inner (3..10))
}
