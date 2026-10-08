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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Reselect Last (`\\gS`): the regions of the last session
 */
class VisualMultiReselectLastTest : VisualMultiTestCase() {

  @Test
  fun `test reselect regions after exit`() {
    configureByText("${c}qwe asd qwe")

    typeText("<C-n><C-n>" + "<Esc>" + "<Bslash><Bslash>gS")

    assertState("${s}qwe$se asd ${s}qwe$se")
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }

  @Test
  fun `test reselect regions after moving`() {
    configureByText(
      """${c}qwe asd qwe
      |zxc
      """.trimMargin(),
    )

    typeText("<C-n><C-n>" + "<Esc>" + "j" + "<Bslash><Bslash>gS")

    assertState(
      """${s}qwe$se asd ${s}qwe$se
      |zxc
      """.trimMargin(),
    )
  }

  @Test
  fun `test n continues the search after reselect`() {
    configureByText("${c}qwe asd qwe asd qwe")

    typeText("<C-n><C-n>" + "<Esc>" + "<Bslash><Bslash>gS" + "n")

    assertState("${s}qwe$se asd ${s}qwe$se asd ${s}qwe$se")
  }

  @Test
  fun `test reselect starts session`() {
    configureByText("${c}qwe asd qwe")

    typeText("<C-n><C-n>" + "<Esc>" + "<Bslash><Bslash>gS")
    assertEquals(2, fixture.editor.caretModel.caretCount)

    // Inside the session, <Esc> exits it and keeps one cursor
    typeText("<Esc>")
    assertEquals(1, fixture.editor.caretModel.caretCount)
  }

  @Test
  fun `test nothing to reselect without previous session`() {
    configureByText("q${c}we asd qwe")

    typeText("<Bslash><Bslash>gS")

    assertState("q${c}we asd qwe")
    assertMode(Mode.NORMAL())
  }

  @Test
  fun `test VM_maps changes Reselect Last key`() {
    configureByText("${c}qwe asd qwe")
    reinitAfter("let g:VM_maps = {}", "let g:VM_maps['Reselect Last'] = '<C-s>'")

    typeText("<C-n><C-n>" + "<Esc>" + "<C-s>")

    assertState("${s}qwe$se asd ${s}qwe$se")
  }
}
