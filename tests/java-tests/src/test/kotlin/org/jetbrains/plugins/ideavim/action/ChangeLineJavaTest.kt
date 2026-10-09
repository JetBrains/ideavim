/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action

import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimJavaTestCase
import org.junit.jupiter.api.Test

/**
 * `cc` and `S` on an empty or blank line in a file that has a language aware indent.
 *
 * Vim does not just keep the indent the line already had: `op_change()` (ops.c) calls `fix_indent()` (indent.c) for a
 * linewise change, which reindents the emptied line with 'cindent' or 'indentexpr'. The caret therefore ends up at the
 * indent of the surrounding code, the same one `o` on the line above would give, however much white space the line
 * held before.
 */
@TestWithoutNeovim(
  reason = SkipNeovimReason.INTELLIJ_PLATFORM_INHERITED_DIFFERENCE,
  description = "The Platform indents by the code style of the language, Neovim by 'shiftwidth'",
)
class ChangeLineJavaTest : VimJavaTestCase() {

  /**
   * The lines are joined by hand rather than written as a raw string, because an indent on a line of its own is
   * trailing white space, which an editor may silently strip from the expected text.
   */
  private fun code(fieldGap: String, bodyGap: String) = listOf(
    "class Main {", "    int a = 1;", fieldGap, "    void f() {", bodyGap, "    }", "}",
  ).joinToString("\n")

  @Test
  fun `test cc on empty line in class body indents to the members`() {
    configureByJavaText(code(fieldGap = "$c", bodyGap = ""))
    typeText("ccy<Esc>")
    assertState(code(fieldGap = "    ${c}y", bodyGap = ""))
  }

  @Test
  fun `test cc on empty line in method body indents to the statements`() {
    configureByJavaText(code(fieldGap = "", bodyGap = "$c"))
    typeText("ccy<Esc>")
    assertState(code(fieldGap = "", bodyGap = "        ${c}y"))
  }

  @Test
  fun `test S on empty line in method body indents to the statements`() {
    configureByJavaText(code(fieldGap = "", bodyGap = "$c"))
    typeText("Sy<Esc>")
    assertState(code(fieldGap = "", bodyGap = "        ${c}y"))
  }

  @Test
  fun `test cc on blank line with too much indent fixes the indent`() {
    configureByJavaText(code(fieldGap = "$c                    ", bodyGap = ""))
    typeText("ccy<Esc>")
    assertState(code(fieldGap = "    ${c}y", bodyGap = ""))
  }

  @Test
  fun `test cc on blank line with too little indent fixes the indent`() {
    configureByJavaText(code(fieldGap = "", bodyGap = "$c  "))
    typeText("ccy<Esc>")
    assertState(code(fieldGap = "", bodyGap = "        ${c}y"))
  }

  @Test
  fun `test cc on blank line indented with tabs fixes the indent`() {
    configureByJavaText(code(fieldGap = "", bodyGap = "$c\t\t\t\t\t"))
    typeText("ccy<Esc>")
    assertState(code(fieldGap = "", bodyGap = "        ${c}y"))
  }

  // `fixthisline()` (indent.c) leaves the indent removable, as for any other automatic indent
  @Test
  fun `test cc and immediate escape leaves an empty line`() {
    configureByJavaText(code(fieldGap = "", bodyGap = "$c  "))
    typeText("cc<Esc>")
    assertState(code(fieldGap = "", bodyGap = "$c"))
  }

  @Test
  fun `test undo after cc restores the line in a single step`() {
    configureByJavaText(code(fieldGap = "", bodyGap = "$c  "))
    typeText("ccy<Esc>")
    typeText("u")
    assertState(code(fieldGap = "", bodyGap = "$c  "))
  }

  @Test
  fun `test cc with a linewise motion indents the emptied line`() {
    configureByJavaText(
      listOf("class Main {", "    void f() {", "$c  int a = 1;", "        int b = 2;", "    }", "}").joinToString("\n"),
    )
    typeText("cjy<Esc>")
    assertState(listOf("class Main {", "    void f() {", "        ${c}y", "    }", "}").joinToString("\n"))
  }

  @Test
  fun `test repeating cc indents every line by its own context`() {
    configureByJavaText(code(fieldGap = "$c", bodyGap = ""))
    typeText("ccy<Esc>", "jj", ".")
    assertState(code(fieldGap = "    y", bodyGap = "        ${c}y"))
  }

  @Test
  fun `test cc with multiple carets indents every line by its own context`() {
    configureByJavaText(code(fieldGap = "$c", bodyGap = "$c"))
    typeText("ccy<Esc>")
    assertState(code(fieldGap = "    ${c}y", bodyGap = "        ${c}y"))
  }
}
