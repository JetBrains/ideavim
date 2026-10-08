/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.visualmulti

import com.maddyhome.idea.vim.state.mode.Mode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * `g:VM_quit_after_leaving_insert_mode`: leaving Insert mode exits the session
 */
class VisualMultiQuitAfterInsertTest : VisualMultiTestCase() {

  @Test
  fun `test leaving insert mode exits session`() {
    configureByText("${c}qwe asd qwe")
    enterCommand("let g:VM_quit_after_leaving_insert_mode = 1")

    typeText("<C-n><C-n>" + "cfoo<Esc>")

    assertState("foo asd fo${c}o")
    assertEquals(1, fixture.editor.caretModel.caretCount)
    assertMode(Mode.NORMAL())
  }

  @Test
  fun `test leaving insert mode keeps session by default`() {
    configureByText("${c}qwe asd qwe")

    typeText("<C-n><C-n>" + "cfoo<Esc>")

    assertState("fo${c}o asd fo${c}o")
  }

  @Test
  fun `test leaving insert mode keeps session when option is zero`() {
    configureByText("${c}qwe asd qwe")
    enterCommand("let g:VM_quit_after_leaving_insert_mode = 0")

    typeText("<C-n><C-n>" + "cfoo<Esc>")

    assertEquals(2, fixture.editor.caretModel.caretCount)
  }
}
