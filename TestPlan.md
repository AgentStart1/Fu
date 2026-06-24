# 测试用例设计

> 目标模块：`rich-text-edit`（核心富文本逻辑）。
> 运行方式：纯逻辑用普通 JUnit（`src/test`），依赖 `android.text.*` 的用 **Robolectric**（同样放 `src/test`，`@RunWith(RobolectricTestRunner)` + `@Config(sdk = [34])`）。
> 说明列「类型」：**Pure** = 普通 JUnit；**Robo** = 需要 Robolectric。

---

## 1. Range 纯逻辑运算（`Range.kt`） — Pure

被测：`cover` / `coerce` / `leftPartial` / `rightPartial` / `partial` / `inner`。重点在边界（端点相等、相邻、包含/被包含）。

定义回顾：
- `A cover B` = `A.start <= B.start && B.end <= A.end`（A 包含 B，含端点相等）
- `A leftPartial B` = `A.start < B.start && B.start < A.end && A.end < B.end`（A 跨过 B 左边界，右端落在 B 内部）
- `A rightPartial B` = `A.start < B.end && B.end < A.end`（A 右端越过 B 右边界）
- `A inner B` = `B.start < A.start && A.end < B.end`（A 严格落在 B 内部）
- `A coerce B` = `max(A.start,B.start)..min(A.end,B.end)`

| 用例 | 场景 | 输入 | 预期 |
|---|---|---|---|
| R-cover-1 | 完全包含 | `(0..10) cover (2..5)` | true |
| R-cover-2 | 端点相等仍算包含 | `(0..10) cover (0..10)` | true |
| R-cover-3 | 被包含方更大 | `(2..5) cover (0..10)` | false |
| R-cover-4 | 部分重叠不算包含 | `(0..5) cover (3..8)` | false |
| R-coerce-1 | 收窄到上界 | `(0..10) coerce (3..7)` | 端点 `3..7`（返回泛型 `ComparableRange`，按端点比较）|
| R-coerce-2 | 一侧超出 | `(0..10) coerce (5..20)` | 端点 `5..10` |
| R-coerce-3 | 完全在内不变 | `(3..7) coerce (0..10)` | 端点 `3..7` |
| R-left-1 | 标准左半重叠 | `(0..5) leftPartial (3..10)` | true |
| R-left-2 | 右端正好等于 B.start（不算） | `(0..3) leftPartial (3..10)` | false |
| R-left-3 | A 完全包含 B（不算 leftPartial） | `(0..20) leftPartial (3..10)` | false |
| R-right-1 | 标准右半重叠 | `(5..15) rightPartial (3..10)` | true |
| R-right-2 | A.start 等于 B.end（不算） | `(10..15) rightPartial (3..10)` | false |
| R-partial-1 | 仅左半 | `(0..5) partial (3..10)` | true |
| R-partial-2 | 仅右半 | `(5..15) partial (3..10)` | true |
| R-partial-3 | 完全包含 | `(0..20) partial (3..10)` | **true**（rightPartial 只要求 `A.start < B.end < A.end`，全包含也满足）|
| R-partial-4 | 无交集 | `(0..2) partial (3..10)` | false |
| R-inner-1 | 严格内部 | `(4..6) inner (3..10)` | true |
| R-inner-2 | 左端贴边（不算严格内部）| `(3..6) inner (3..10)` | false |
| R-inner-3 | 等于 B | `(3..10) inner (3..10)` | false |

---

## 2. 段落定位 `paragraphAt`（`RichTextEditing.kt`） — Pure

被测：`CharSequence.paragraphAt(selection): Paragraph`，向两侧扩展到 `\n` 或文本边界。

| 用例 | 场景 | 输入 | 预期 |
|---|---|---|---|
| P-1 | 第一段中间（已有覆盖）| `"test\nhello".paragraphAt(1)` | `Paragraph(0,4)` |
| P-2 | 第二段中间（已有覆盖）| `"test\nhello".paragraphAt(6)` | `Paragraph(5,10)` |
| P-3 | 光标在 0 | `"abc".paragraphAt(0)` | `Paragraph(0,3)` |
| P-4 | 光标在末尾 | `"abc".paragraphAt(3)` | `Paragraph(0,3)` |
| P-5 | 无换行整段 | `"hello".paragraphAt(2)` | `Paragraph(0,5)` |
| P-6 | 空字符串 | `"".paragraphAt(0)` | `Paragraph(0,0)` |
| P-7 | 光标正好落在 `\n` 上 | `"a\nb".paragraphAt(1)` | `Paragraph(0,1)`（向右遇到 `\n` 停） |
| P-8 | 光标在 `\n` 之后段首 | `"a\nb".paragraphAt(2)` | `Paragraph(2,3)` |
| P-9 | 连续空行 | `"a\n\nb".paragraphAt(2)` | `Paragraph(2,2)`（空段落）|

---

## 3. toggle 样式应用/移除（`RichTextEditing.kt`） — Robo

构造：`SpannableStringBuilder().apply { append(...) }`，对其调用 `toggle`。

### 3.1 字符型样式（`RichTextStyle`，以 `BoldStyle` 为主）

| 用例 | 场景 | 前置/操作 | 预期 |
|---|---|---|---|
| T-char-1 | 首次应用 | `"hello"`，`toggle(0..4, Bold)` | 区间内有 1 个 Bold，范围 `0..4`（已有覆盖）|
| T-char-2 | 再次切换移除 | 在 T-char-1 后再 `toggle(0..4, Bold)` | Bold 为空（已有覆盖）|
| T-char-3 | 合并两段相邻同样式 | `toggle(0..1)`、`toggle(2..3)` 后 `toggle(0..3)` | 合并为 1 个 Bold，范围 `0..3`（已有覆盖）|
| T-char-4 | 选区被已有更大样式完整覆盖 → 关闭并裁切 | `toggle(0..5)` 后 `toggle(1..3)` | 原 span 被裁切成 `0..1` 与 `3..5` 两段，中间无样式 |
| T-char-5 | 选区部分左重叠 | `"hello"`，`toggle(0..2)` 后 `toggle(1..4)` | 结果为覆盖 `0..4` 的单一 Bold（未覆盖处补齐）|
| T-char-6 | 选区内部 inner 的样式被移除 | 在 `1..2` 有 Bold，`toggle(0..4)` | 内部小 span 移除，最终一个覆盖更大范围的 span |

### 3.2 多值样式（`MultiValueStyle`，以 `ColorStyle` 为主）

| 用例 | 场景 | 前置/操作 | 预期 |
|---|---|---|---|
| T-mv-1 | 应用颜色后检测存在（已有覆盖）| `toggle(0..2, Color(RED))` | `detectStyle(0..2)` 含 1 项 |
| T-mv-2 | 同值重复应用保持存在（已有覆盖）| 再 `toggle(0..2, Color(RED))` | `detectStyle(0..2)` 仍为 1 项 |
| T-mv-3 | 改值 | `toggle(0..2, Color(RED))` 后 `toggle(0..2, Color(BLUE))` | 该区间生效颜色为 BLUE（旧值被替换）|
| T-mv-4 | clear 清除 | 应用后 `clear(ColorStyle)` | 区间内无 ColorStyle |

### 3.3 段落型样式（`RichParagraphStyle`）

| 用例 | 场景 | 前置/操作 | 预期 |
|---|---|---|---|
| T-para-1 | Headline 应用到整段（已有覆盖）| `"hello"`，`toggle(0..2, Headline(1))` | span 覆盖整段 `0..5`，`detectStyle` 含 1 项 |
| T-para-2 | 不同 Headline 值替换（已有覆盖）| 再 `toggle(0..2, Headline(2))` | 仍 1 项，值变为 2 |
| T-para-3 | 同值再次切换移除 | `toggle(0..2, Headline(1))` 两次 | 段落无 Headline |
| T-para-4 | Alignment 应用 | `toggle(0..2, Alignment(OPPOSITE))` | 段落范围出现对应 AlignmentStyle |
| T-para-5 | 多段落只作用于光标所在段 | `"aaa\nbbb"`，光标在第 2 段 `toggle Headline` | 仅第 2 段被设置 |

### 3.4 冲突样式（`conflict`）

| 用例 | 场景 | 前置/操作 | 预期 |
|---|---|---|---|
| T-conf-1 | Headline 声明与 Bold 冲突 | 区间已有 Bold，`toggle(Headline)` | `HeadlineStyle.conflict == [Bold]`，行为按 `toggleText` 中的 conflict 早退判定（**待与作者确认实际预期**，见末尾 Q1）|

---

## 4. detectStyle / detectCoveredStyle（`RichTextEditing.kt`） — Robo

被测：`detectStyle(range)` 返回区间内「完整覆盖且未被 Break 打断且不因尾部标点而失效」的样式列表。

| 用例 | 场景 | 前置/操作 | 预期 |
|---|---|---|---|
| D-1 | 完整覆盖被检出 | `toggle(0..2, Color(RED))`，`detectStyle(0..2)` | 1 项（已有覆盖）|
| D-2 | 尾字符为标点 → 光标处不继承 | `"hello!"`，`toggle(0..6, Bold)`，`detectStyle(6..6)` | 0 项（已有覆盖 testAutoApplyTextStyle）|
| D-3 | 尾字符非标点 → 继承 | `"hello"`，`toggle(0..5, Bold)`，`detectStyle(5..5)` | 1 项 |
| D-4 | 仅部分覆盖不检出 | `toggle(0..2, Bold)`，`detectStyle(0..4)` | 0 项（未完整覆盖）|
| D-5 | Break 打断的样式被排除 | 构造含 `Break(Bold)` 覆盖样式范围，`detectStyle` | 该样式不计入 |
| D-6 | 多样式同时检出 | 同区间叠加 Bold + Color，`detectStyle` | 2 项 |
| D-7 | 空区间/无样式 | 无样式文本 `detectStyle(0..0)` | 0 项 |

> 备注：D-5 需要能构造 `Break` span。Spec 描述 Break 由反选光标插入，单测里需手动 `setSpan(Break(BoldStyle::class.java as Class<RichSpan>), ...)` 来模拟，**构造方式待确认**（见末尾 Q2）。

---

## 5. Stringify 序列化（`Stringify.kt`） — Robo

被测：`Spanned.richFormatPlain()` 与 `Context.parseRichFormatPlain(text, json)`。

| 用例 | 场景 | 前置/操作 | 预期 |
|---|---|---|---|
| S-1 | 单 Headline 序列化（已有覆盖）| `toggle(0..2, Headline(1))` → `richFormatPlain()` | `[{"type":"headline","start":0,"end":5,"data":1}]` |
| S-2 | Alignment 序列化（已有覆盖）| `toggle(0..2, Alignment(OPPOSITE))` | `[{"type":"align","start":0,"end":5,"data":1}]` |
| S-3 | 往返一致（解析回 span）| S-1 的 json `parseRichFormatPlain` | 区间含 1 个 span（已有覆盖 testCharSequenceStringify）|
| S-4 | 颜色 data 字段 | `toggle(0..2, Color(RED))` 序列化 | `data` == `Color.RED` 的 Int 值，`type=="color"` |
| S-5 | 无样式 | 纯文本 `richFormatPlain()` | `"[]"` |
| S-6 | 多样式序列化 | Bold(0..2) + Color(1..3) | 输出含 2 条，type/start/end 正确 |
| S-7 | 非多值样式 data 为 0 | Bold 序列化 | `data == 0` |
| S-8 | 未知 type 解析抛异常 | 手写非法 type 的 json | `parseRichFormatPlain` 抛 `Exception("unrecognized type ...")` |

---

## 待确认问题

- **Q1（冲突语义）**：`toggleText` 里 `conflict.any { resolveStyleFillResult(...).isNotEmpty() }` 检测的是「当前样式自身」是否已填充，而非遍历 `conflict` 列表里的冲突类型 —— 看起来逻辑没真正用到 `it`（冲突类）。这是预期行为还是潜在 bug？我应按现状写「黑盒预期」，还是按你描述的「冲突时不应用」写预期（可能会暴露 bug）？
- **Q2（Break 构造）**：单测里模拟 Break 的方式（直接 `setSpan(Break(...))`）你是否认可？还是更希望通过 `RichEditText` 的真实交互路径触发？
- **Q3（落点）**：Robolectric 测试与现有 `androidTest/ExampleInstrumentedTest` 是否合并/迁移到 `src/test`？还是新建独立测试类、保留现有 instrumented 测试不动？

---

## 落地（确认后）依赖与脚手架

- `rich-text-edit/build.gradle.kts` 增加：`testImplementation("org.robolectric:robolectric:<ver>")`、`testImplementation(libs.androidx.test.ext.junit)`、`testImplementation("androidx.test:core:<ver>")`；`android { testOptions { unitTests.isIncludeAndroidResources = true } }`。
- 纯逻辑测试类：`RangeTest`、`ParagraphTest`（无 Robolectric）。
- Robolectric 测试类：`ToggleTest`、`DetectStyleTest`、`StringifyTest`（`@RunWith(RobolectricTestRunner)`、`@Config(sdk=[34])`、`ApplicationProvider` 取 Context）。
