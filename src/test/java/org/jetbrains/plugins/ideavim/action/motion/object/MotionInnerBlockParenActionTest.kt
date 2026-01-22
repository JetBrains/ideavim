/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.`object`

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class MotionInnerBlockParenActionTest : VimTestCase() {
  // VIM-1633 |v_i)|
  @Test
  fun `test single letter with single parentheses`() {
    configureByText("(${c}a)")
    typeText(injector.parser.parseKeys("vi)"))
    assertSelection("a")
  }

  @Test
  fun `test single letter with double parentheses`() {
    configureByText("((${c}a))")
    typeText(injector.parser.parseKeys("vi)"))
    assertSelection("(a)")
  }

  @Test
  fun `test multiline outside parentheses`() {
    configureByText(
      """(outer
                        |$c(inner))
      """.trimMargin(),
    )
    typeText(injector.parser.parseKeys("vi)"))
    assertSelection("inner")
  }

  @Test
  fun `test multiline in parentheses`() {
    configureByText(
      """(outer
                        |(inner$c))
      """.trimMargin(),
    )
    typeText(injector.parser.parseKeys("vi)"))
    assertSelection("inner")
  }

  @Test
  fun `test multiline inside of outer parentheses`() {
    configureByText(
      """(outer
                         |$c (inner))
      """.trimMargin(),
    )
    typeText(injector.parser.parseKeys("vi)"))
    assertSelection(
      """outer
                        | (inner)
      """.trimMargin(),
    )
  }

  @Test
  fun `test double motion`() {
    configureByText(
      """(outer
                      |$c(inner))
      """.trimMargin(),
    )
    typeText(injector.parser.parseKeys("vi)i)"))
    assertSelection(
      """outer
                          |(inner)
      """.trimMargin(),
    )
  }

  @Test
  fun `test double motion parentheses start end are not line break`() {
    configureByText(
      """(outer(b
        |${c}inner
        |b)outer)
      """.trimMargin(),
    )
    typeText(injector.parser.parseKeys("vi)i)"))
    assertSelection(
      """outer(b
        |inner
        |b)outer
      """.trimMargin(),
    )
  }

  @Test
  fun `test double motion parentheses start end are line break`() {
    configureByText(
      """(outer(
        |${c}inner
        |)outer)
      """.trimMargin(),
    )
    typeText(injector.parser.parseKeys("vi)i)"))
    assertSelection(
      """outer(
        |inner
        |)outer
      """.trimMargin(),
    )
  }

  @Test
  fun `test double motion parentheses start is line break end is not`() {
    configureByText(
      """(outer(
        |${c}inner
        |b)outer)
      """.trimMargin(),
    )
    typeText(injector.parser.parseKeys("vi)i)"))
    assertSelection(
      """outer(
        |inner
        |b)outer
      """.trimMargin(),
    )
  }

  @Test
  fun `test double motion parentheses end is line break start is not`() {
    configureByText(
      """(outer(b
         |${c}inner
         |)outer)
      """.trimMargin(),
    )
    typeText(injector.parser.parseKeys("vi)i)"))
    assertSelection(
      """outer(b
        |inner
        |)outer
      """.trimMargin(),
    )
  }

  @Test
  fun `test motion with count`() {
    configureByText(
      """(outer
                          |$c(inner))
      """.trimMargin(),
    )
    typeText(injector.parser.parseKeys("v2i)"))
    assertSelection(
      """outer
                      |(inner)
      """.trimMargin(),
    )
  }

  @Test
  fun `test text object after motion`() {
    configureByText(
      """(outer
                      |$c(inner))
      """.trimMargin(),
    )
    typeText(injector.parser.parseKeys("vlli)"))
    assertSelection(
      """outer
                      |(inner)
      """.trimMargin(),
    )
  }

  @Test
  fun `test text object after motion outside parentheses`() {
    configureByText(
      """(outer
                      |(inner$c))
      """.trimMargin(),
    )
    typeText(injector.parser.parseKeys("vlli)"))
    assertSelection("inner")
  }

  @Test
  fun `test text object after motion inside parentheses`() {
    configureByText(
      """(outer
                      |(${c}inner))
      """.trimMargin(),
    )
    typeText(injector.parser.parseKeys("vllli)"))
    assertSelection("inner")
  }

  // VIM-326 |d| |v_ib|
  @Test
  fun testDeleteInnerBlock() {
    typeTextInFile(
      injector.parser.parseKeys("di)"),
      "foo(\"b${c}ar\")\n",
    )
    assertState("foo()\n")
  }

  // VIM-1008 |d| |v_ib|
  @Test
  fun testDeleteInnerBlockWithQuote() {
    typeTextInFile(
      injector.parser.parseKeys("di)"),
      "(abc${c}def'ghi)",
    )
    assertState("()")
  }

  // VIM-1008 |d| |v_ib|
  @Test
  fun testDeleteInnerBlockWithDoubleQuote() {
    typeTextInFile(
      injector.parser.parseKeys("di)"),
      """(abc${c}def"ghi)""",
    )
    assertState("()")
  }

  // VIM-326 |d| |v_ib|
  @Test
  fun testDeleteInnerBlockCaretBeforeString() {
    typeTextInFile(
      injector.parser.parseKeys("di)"),
      "foo(${c}\"bar\")\n",
    )
    assertState("foo()\n")
  }

  // VIM-326 |c| |v_ib|
  @Test
  fun testChangeInnerBlockCaretBeforeString() {
    typeTextInFile(
      injector.parser.parseKeys("ci)"),
      "foo(${c}\"bar\")\n",
    )
    assertState("foo()\n")
  }

  // VIM-392 |c| |v_ib|
  @Test
  fun testChangeInnerBlockCaretBeforeBlock() {
    typeTextInFile(
      injector.parser.parseKeys("ci)"),
      "foo$c(bar)\n",
    )
    assertState("foo()\n")
    assertOffset(4)
  }

  // |v_ib|
  @Test
  fun testInnerBlockCrashWhenNoDelimiterFound() {
    typeTextInFile(injector.parser.parseKeys("di)"), "(x\n")
    assertState("(x\n")
  }

  // VIM-275 |d| |v_ib|
  @Test
  fun testDeleteInnerParensBlockBeforeOpen() {
    typeTextInFile(
      injector.parser.parseKeys("di)"),
      "foo$c(bar)\n",
    )
    assertState("foo()\n")
    assertOffset(4)
  }

  // |d| |v_ib|
  @Test
  fun testDeleteInnerParensBlockBeforeClose() {
    typeTextInFile(
      injector.parser.parseKeys("di)"),
      "foo(bar$c)\n",
    )
    assertState("foo()\n")
  }

  @Test
  fun testOutside() {
    typeTextInFile(
      injector.parser.parseKeys("di)"),
      "${c}foo(bar)\n",
    )
    assertState("foo()\n")
  }

  @Test
  fun testOutsideInString() {
    typeTextInFile(
      injector.parser.parseKeys("di)"),
      "\"1${c}23\"foo(bar)\n",
    )
    assertState("\"123\"foo()\n")
  }

  @Test
  fun testOutsideInString2() {
    typeTextInFile(
      injector.parser.parseKeys("di)"),
      "\"1${c}23(dsa)d\"foo(bar)\n",
    )
    assertState("\"123()d\"foo(bar)\n")
  }

  // ============== preserveSelectionAnchor behavior tests ==============

  @Test
  fun `test inner paren from middle of content`() {
    doTest(
      "vi)",
      "foo (bar b${c}az qux) quux",
      "foo (${s}bar baz qu${c}x${se}) quux",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  @VimBehaviorDiffers(
    shouldBeFixed = false,
    description = """
      Vim for some operations keeps the direction and for some it doesn't.
      However, this looks like a bug in Vim.
      So, in IdeaVim we always keep the direction.
    """
  )
  fun `test inner paren with backwards selection`() {
    doTest(
      listOf("v", "h", "i)"),
      "foo (bar b${c}az qux) quux",
      "foo (${s}${c}bar baz qux${se}) quux",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  @VimBehaviorDiffers(
    shouldBeFixed = false,
    description = """
      Vim for some operations keeps the direction and for some it doesn't.
      However, this looks like a bug in Vim.
      So, in IdeaVim we always keep the direction.
    """
  )
  fun `test inner paren with backwards selection crossing boundary`() {
    // Move selection back past opening paren
    doTest(
      listOf("v", "F(", "i)"),
      "foo (bar b${c}az qux) quux",
      "foo (${s}${c}bar baz qux${se}) quux",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }
}
