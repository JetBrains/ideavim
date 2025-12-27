/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.insert

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class InsertRegisterLiterallyActionTest : VimTestCase() {
  @Test
  fun `test insert named register literally with CTRL-R CTRL-R`() {
    configureByText("\n")
    enterCommand("let @a=\"hello world\"")
    typeText("i<C-R><C-R>a")
    assertState("hello world${c}\n")
  }

  @Test
  fun `test insert named register literally with CTRL-R CTRL-O`() {
    configureByText("\n")
    enterCommand("let @a=\"hello world\"")
    typeText("i<C-R><C-O>a")
    assertState("hello world${c}\n")
  }

  @Test
  fun `test insert named register literally with CTRL-R CTRL-R after existing text`() {
    configureByText("${c}\n")
    enterCommand("let @a=\"world\"")
    typeText("ihello <C-R><C-R>a")
    assertState("hello world${c}\n")
  }

  @Test
  fun `test insert named register literally with CTRL-R CTRL-O after existing text`() {
    configureByText("${c}\n")
    enterCommand("let @a=\"world\"")
    typeText("ihello <C-R><C-O>a")
    assertState("hello world${c}\n")
  }

  @Test
  fun `test insert default register literally with CTRL-R CTRL-R`() {
    configureByText("${c}hello\n")
    typeText("ywo<C-R><C-R>\"")
    assertState("hello\nhello${c}\n")
  }

  @Test
  fun `test insert default register literally with CTRL-R CTRL-O`() {
    configureByText("${c}hello\n")
    typeText("ywo<C-R><C-O>\"")
    assertState("hello\nhello${c}\n")
  }

  @Test
  fun `test insert numbered register literally with CTRL-R CTRL-R`() {
    configureByText("${c}line1\nline2\n")
    typeText("yyjo<C-R><C-R>0")
    assertState("line1\nline2\nline1\n${c}\n")
  }

  @Test
  fun `test insert numbered register literally with CTRL-R CTRL-O`() {
    configureByText("${c}line1\nline2\n")
    typeText("yyjo<C-R><C-O>0")
    assertState("line1\nline2\nline1\n${c}\n")
  }
}
