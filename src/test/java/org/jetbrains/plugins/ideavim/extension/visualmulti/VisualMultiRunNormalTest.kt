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
 * Run Normal and Run Last Normal (`\\z` and `\\Z`): a `:normal` command at each cursor
 */
class VisualMultiRunNormalTest : VisualMultiTestCase() {

  private val text = """${c}abc
    |def
    |ghi
  """.trimMargin()

  @Test
  fun `test run normal command at each cursor`() {
    configureByText(text)

    typeText("<C-Down>" + "<Bslash><Bslash>z" + "A!<CR>")

    assertState(
      """abc${c}!
      |def${c}!
      |ghi
      """.trimMargin(),
    )
    assertMode(Mode.NORMAL())
  }

  @Test
  fun `test run normal command at the end of each region`() {
    configureByText("${c}qwe asd qwe")

    typeText("<C-n><C-n>" + "<Bslash><Bslash>z" + "x<CR>")

    assertState("qw$c asd q${c}w")
    assertMode(Mode.NORMAL())
  }

  @Test
  fun `test run normal command uses mappings`() {
    configureByText(text)
    enterCommand("nmap ,d dd")

    typeText("<C-Down>" + "<Bslash><Bslash>z" + ",d<CR>")

    assertState("${c}ghi")
  }

  @Test
  fun `test empty normal command does nothing`() {
    configureByText(text)

    typeText("<C-Down>" + "<Bslash><Bslash>z" + "<CR>")

    assertState(
      """${c}abc
      |${c}def
      |ghi
      """.trimMargin(),
    )
  }

  @Test
  fun `test run last normal command`() {
    configureByText(text)

    typeText("<C-Down>" + "<Bslash><Bslash>z" + "A!<CR>" + "<Bslash><Bslash>Z")

    assertState(
      """abc!${c}!
      |def!${c}!
      |ghi
      """.trimMargin(),
    )
  }

  @Test
  fun `test VM_maps changes Run Normal key`() {
    configureByText(text)
    reinitAfter("let g:VM_maps = {}", "let g:VM_maps['Run Normal'] = '<C-z>'")

    typeText("<C-Down>" + "<C-z>" + "A!<CR>")

    assertState(
      """abc${c}!
      |def${c}!
      |ghi
      """.trimMargin(),
    )
  }
}
