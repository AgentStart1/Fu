package com.storyteller_f.fuluent.rich_text_edit

import android.text.SpannableStringBuilder

/**
 * 测试用：构造一个 SpannableStringBuilder。
 */
internal fun builder(block: SpannableStringBuilder.() -> Unit): SpannableStringBuilder =
    SpannableStringBuilder().apply(block)
