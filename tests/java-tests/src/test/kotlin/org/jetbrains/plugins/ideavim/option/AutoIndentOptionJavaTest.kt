/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.option

import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimJavaTestCase
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * Tests for the 'autoindent' option in a file that has a language aware indent.
 *
 * 'autoindent' only copies the indent of the current line. An indent computed by 'cindent', 'indentexpr' or
 * 'smartindent' is added regardless of the option - the `do_cindent` condition in `open_line()` (change.c) does not
 * look at `b_p_ai` at all. The IntelliJ Platform always computes the indent of a new line from the language, so it is
 * the counterpart of 'indentexpr', not of 'autoindent', and `:set noautoindent` must not remove it.
 *
 * Where IdeaVim parts company with Vim is what happens when Insert mode is left without anything being typed. Vim
 * deletes the indent again, for both kinds (`fixthisline()` in indent.c sets `did_ai` when the line it has indented is
 * white), so that `o<Esc>` leaves an empty line. IdeaVim keeps it, because a line opened in the IDE is expected to
 * stay where it is. `cc` and `S` are the exception and still follow Vim.
 *
 * The expected results were captured from `nvim --clean` with `set noautoindent` on a `.java` file, which loads
 * `indent/java.vim` and therefore has `cindent` and `indentexpr=GetJavaIndent()` set. The counterpart of these tests
 * for a buffer with no language indent is [AutoIndentOptionTest], which uses a plain text file.
 */
@TestWithoutNeovim(
  reason = SkipNeovimReason.INTELLIJ_PLATFORM_INHERITED_DIFFERENCE,
  description = "The Platform indents by the code style of the language, Neovim by 'shiftwidth'. Leaving Insert mode " +
    "without typing anything keeps the indent, which Vim deletes",
)
class AutoIndentOptionJavaTest : VimJavaTestCase() {

  private val indent = " ".repeat(8)

  private fun configureIndentedStatement() {
    configureByJavaText(
      """
            class Main {
                void f() {
                    int ${c}x = 1;
                }
            }
      """.trimIndent(),
    )
  }

  /**
   * The lines are joined by hand rather than written as a raw string, because an indent that is kept on a line of its
   * own is trailing white space, which an editor may silently strip from the expected text.
   */
  private fun codeWith(line: String, addedAt: Int) = mutableListOf(
    "class Main {", "    void f() {", "        int x = 1;", "    }", "}",
  ).apply { add(addedAt, line) }.joinToString("\n")

  private fun codeWithLine2ReplacedBy(line: String) = mutableListOf(
    "class Main {", "    void f() {", line, "    }", "}",
  ).joinToString("\n")

  private fun assertKeptIndentAt(line: Int) {
    assertState(codeWith(indent, addedAt = line))
    assertPosition(line, indent.length - 1)
  }

  // `o`, `O` and Enter - the indent comes from the IDE and must survive 'noautoindent'

  @Test
  fun `test o keeps the language indent when autoindent is off`() {
    configureIndentedStatement()
    enterCommand("set noautoindent")
    typeText("oy<Esc>")
    assertState(codeWith("$indent${c}y", addedAt = 3))
  }

  @Test
  fun `test O keeps the language indent when autoindent is off`() {
    configureIndentedStatement()
    enterCommand("set noautoindent")
    typeText("Oy<Esc>")
    assertState(codeWith("$indent${c}y", addedAt = 2))
  }

  @Test
  fun `test enter in insert mode keeps the language indent when autoindent is off`() {
    configureIndentedStatement()
    enterCommand("set noautoindent")
    typeText("A<CR>y<Esc>")
    assertState(codeWith("$indent${c}y", addedAt = 3))
  }

  // Leaving Insert mode without typing anything keeps the indent, unlike Vim

  @Test
  fun `test o and immediate escape keeps the indent when autoindent is off`() {
    configureIndentedStatement()
    enterCommand("set noautoindent")
    typeText("o<Esc>")
    assertKeptIndentAt(3)
  }

  @Test
  fun `test O and immediate escape keeps the indent when autoindent is off`() {
    configureIndentedStatement()
    enterCommand("set noautoindent")
    typeText("O<Esc>")
    assertKeptIndentAt(2)
  }

  @Test
  fun `test enter and immediate escape keeps the indent when autoindent is off`() {
    configureIndentedStatement()
    enterCommand("set noautoindent")
    typeText("A<CR><Esc>")
    assertKeptIndentAt(3)
  }

  @Test
  fun `test o and immediate escape keeps the indent when autoindent is on`() {
    configureIndentedStatement()
    typeText("o<Esc>")
    assertKeptIndentAt(3)
  }

  // `cc` and `S` - Vim puts the caret in column 0, but 'cindent' then indents the emptied line again

  @Disabled(
    "`cc` and `S` do not ask the IDE for an indent at all - they keep the indent the line already had, or put the " +
      "caret in column 0. Vim empties the line and lets 'cindent' indent it again, so the text ends up indented " +
      "even with 'noautoindent'. Enabling this test means reindenting the emptied line through the Platform"
  )
  @Test
  fun `test cc keeps the language indent when autoindent is off`() {
    configureIndentedStatement()
    enterCommand("set noautoindent")
    typeText("ccy<Esc>")
    assertState(codeWithLine2ReplacedBy("$indent${c}y"))
  }

  @Disabled(
    "`cc` and `S` do not ask the IDE for an indent at all - they keep the indent the line already had, or put the " +
      "caret in column 0. Vim empties the line and lets 'cindent' indent it again, so the text ends up indented " +
      "even with 'noautoindent'. Enabling this test means reindenting the emptied line through the Platform"
  )
  @Test
  fun `test S keeps the language indent when autoindent is off`() {
    configureIndentedStatement()
    enterCommand("set noautoindent")
    typeText("Sy<Esc>")
    assertState(codeWithLine2ReplacedBy("$indent${c}y"))
  }

  @Test
  fun `test cc and immediate escape leaves an empty line when autoindent is off`() {
    configureIndentedStatement()
    enterCommand("set noautoindent")
    typeText("cc<Esc>")
    assertState(codeWithLine2ReplacedBy("$c"))
  }

  // 'autoindent' on is the default and must keep working

  @Test
  fun `test o indents the new line when autoindent is on`() {
    configureIndentedStatement()
    typeText("oy<Esc>")
    assertState(codeWith("$indent${c}y", addedAt = 3))
  }
}
