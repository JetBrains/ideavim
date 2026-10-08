/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.visualmulti

import com.maddyhome.idea.vim.state.mode.Mode
import org.junit.jupiter.api.Test

/**
 * Run Macro (`\\@`): a macro at each cursor
 */
class VisualMultiRunMacroTest : VisualMultiTestCase() {

  private val text = """${c}abc
    |def
    |ghi
  """.trimMargin()

  @Test
  fun `test run macro at each cursor`() {
    configureByText(text)
    enterCommand("let @a = 'A!'")

    typeText("<C-Down>" + "<Bslash><Bslash>@" + "a")

    assertState(
      """abc${c}!
      |def${c}!
      |ghi
      """.trimMargin(),
    )
    assertMode(Mode.NORMAL())
  }

  @Test
  fun `test run macro turns regions into cursors first`() {
    configureByText("${c}qwe asd qwe")
    enterCommand("let @a = 'x'")

    typeText("<C-n><C-n>" + "<Bslash><Bslash>@" + "a")

    assertState("${c}we asd ${c}we")
    assertMode(Mode.NORMAL())
  }

  @Test
  fun `test escape aborts run macro`() {
    configureByText(text)
    enterCommand("let @a = 'A!'")

    typeText("<C-Down>" + "<Bslash><Bslash>@" + "<Esc>")

    assertState(
      """${c}abc
      |${c}def
      |ghi
      """.trimMargin(),
    )
  }

  @Test
  fun `test VM_maps changes Run Macro key`() {
    configureByText(text)
    enterCommand("let @a = 'A!'")
    reinitAfter("let g:VM_maps = {}", "let g:VM_maps['Run Macro'] = '<C-q>'")

    typeText("<C-Down>" + "<C-q>" + "a")

    assertState(
      """abc${c}!
      |def${c}!
      |ghi
      """.trimMargin(),
    )
  }
}
