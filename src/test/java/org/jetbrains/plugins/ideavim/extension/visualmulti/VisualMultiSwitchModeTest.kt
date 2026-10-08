/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.visualmulti

import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.junit.jupiter.api.Test

/**
 * Switch Mode (`<Tab>`): between the regions (extend mode, Visual) and the cursors (cursor mode, Normal)
 */
class VisualMultiSwitchModeTest : VisualMultiTestCase() {

  @Test
  fun `test tab turns regions into cursors at their start`() {
    configureByText("${c}qwe asd qwe")

    typeText("<C-n><C-n>" + "<Tab>")

    assertState("${c}qwe asd ${c}qwe")
    assertMode(Mode.NORMAL())
  }

  @Test
  fun `test cursors edit after tab`() {
    configureByText("${c}qwe asd qwe")

    typeText("<C-n><C-n>" + "<Tab>" + "x")

    assertState("${c}we asd ${c}we")
  }

  @Test
  fun `test tab turns cursors into regions of one character`() {
    configureByText(
      """a${c}bc
      |def
      """.trimMargin(),
    )

    typeText("<C-Down>" + "<Tab>")

    assertState(
      """a${s}b${se}c
      |d${s}e${se}f
      """.trimMargin(),
    )
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }

  @Test
  fun `test motion extends all regions after tab`() {
    configureByText(
      """a${c}bc
      |def
      """.trimMargin(),
    )

    typeText("<C-Down>" + "<Tab>" + "l")

    assertState(
      """a${s}bc$se
      |d${s}ef$se
      """.trimMargin(),
    )
  }

  @Test
  fun `test tab twice goes back to regions`() {
    configureByText("${c}qwe asd qwe")

    typeText("<C-n><C-n>" + "<Tab><Tab>")

    assertState("${s}q${se}we asd ${s}q${se}we")
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }

  @Test
  fun `test tab with single region leaves one cursor`() {
    configureByText("q${c}we asd")

    typeText("<C-n>" + "<Tab>")

    assertState("${c}qwe asd")
    assertMode(Mode.NORMAL())
  }

  @Test
  fun `test tab outside session keeps its Vim meaning`() {
    configureByText(
      """${c}a
      |b
      |c
      """.trimMargin(),
    )

    // <Tab> is the same as <C-i>, which goes forward in the jump list
    typeText("G" + "<C-o>" + "<Tab>")

    assertState(
      """a
      |b
      |${c}c
      """.trimMargin(),
    )
  }

  @Test
  fun `test VM_maps changes Switch Mode key`() {
    configureByText("${c}qwe asd qwe")
    reinitAfter("let g:VM_maps = {}", "let g:VM_maps['Switch Mode'] = '<C-t>'")

    typeText("<C-n><C-n>" + "<C-t>")

    assertState("${c}qwe asd ${c}qwe")
  }
}
