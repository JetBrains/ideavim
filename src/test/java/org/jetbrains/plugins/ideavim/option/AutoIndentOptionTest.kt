/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.option

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

/**
 * Tests for the 'autoindent' option.
 *
 * The option is on by default, which matches the Neovim default ('autoindent' is off in Vim, see
 * `runtime/doc/vim_diff.txt`). With 'autoindent' off, `cc`, `S`, `o`, `O` and Enter in Insert mode do not indent the new
 * line at all - Vim puts the caret in column 0 (`beginline(0)` in `op_delete()`, ops.c) instead of keeping the indent.
 *
 * The expected results below were captured from `nvim --clean` with `set noautoindent`.
 */
class AutoIndentOptionTest : VimTestCase() {
  @Test
  fun `test cc does not indent the new line when autoindent is off`() {
    configureByText(
      """
            fun main() {
                ${c}println("hi")
            }
      """.trimIndent(),
    )
    enterCommand("set noautoindent")
    typeText("ccx<Esc>")
    assertState(
      """
            fun main() {
            ${c}x
            }
      """.trimIndent(),
    )
  }

  @Test
  fun `test S does not indent the new line when autoindent is off`() {
    configureByText(
      """
            fun main() {
                ${c}println("hi")
            }
      """.trimIndent(),
    )
    enterCommand("set noautoindent")
    typeText("Sx<Esc>")
    assertState(
      """
            fun main() {
            ${c}x
            }
      """.trimIndent(),
    )
  }

  @Test
  fun `test o does not indent the new line when autoindent is off`() {
    configureByText(
      """
            fun main() {
                ${c}println("hi")
            }
      """.trimIndent(),
    )
    enterCommand("set noautoindent")
    typeText("ox<Esc>")
    assertState(
      """
            fun main() {
                println("hi")
            ${c}x
            }
      """.trimIndent(),
    )
  }

  @Test
  fun `test O does not indent the new line when autoindent is off`() {
    configureByText(
      """
            fun main() {
                ${c}println("hi")
            }
      """.trimIndent(),
    )
    enterCommand("set noautoindent")
    typeText("Ox<Esc>")
    assertState(
      """
            fun main() {
            ${c}x
                println("hi")
            }
      """.trimIndent(),
    )
  }

  @Test
  fun `test enter does not copy the indent when autoindent is off`() {
    configureByText(
      """
            fun main() {
                ${c}println("hi")
            }
      """.trimIndent(),
    )
    enterCommand("set noautoindent")
    typeText("A<CR>x<Esc>")
    assertState(
      """
            fun main() {
                println("hi")
            ${c}x
            }
      """.trimIndent(),
    )
  }

  // With 'autoindent' off there is no indent to remove, so the result of `cc<Esc>` is the same as with 'autoindent' on -
  // only the intermediate state differs
  @Test
  fun `test cc and immediate escape leaves an empty line when autoindent is off`() {
    configureByText(
      """
            fun main() {
                ${c}println("hi")
            }
      """.trimIndent(),
    )
    enterCommand("set noautoindent")
    typeText("cc<Esc>")
    assertState(
      """
            fun main() {
            $c
            }
      """.trimIndent(),
    )
  }

  // The option is local to buffer, so `:setlocal` is enough to change it for the current buffer
  @Test
  fun `test setlocal noautoindent affects the current buffer`() {
    configureByText(
      """
            fun main() {
                ${c}println("hi")
            }
      """.trimIndent(),
    )
    enterCommand("setlocal noautoindent")
    typeText("ccx<Esc>")
    assertState(
      """
            fun main() {
            ${c}x
            }
      """.trimIndent(),
    )
  }

  @Test
  fun `test autoindent is on by default`() {
    configureByText(
      """
            fun main() {
                ${c}println("hi")
            }
      """.trimIndent(),
    )
    typeText("ccx<Esc>")
    assertState(
      """
            fun main() {
                ${c}x
            }
      """.trimIndent(),
    )
  }
}
