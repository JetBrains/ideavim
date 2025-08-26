/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.insert

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class InsertExitModeActionTest : VimTestCase() {
  @Test
  fun `test exit visual mode`() {
    doTest("i<Esc>", "12${c}3", "1${c}23", Mode.NORMAL())
  }

  @Test
  fun `test exit visual mode on line start`() {
    doTest("i<Esc>", "${c}123", "${c}123", Mode.NORMAL())
  }

  @ParameterizedTest
  @ValueSource(strings = ["i", "a", "o", "O"])
  fun `test cannot enter insert mode in read-only file`(insertCommand: String) {
    configureByText("12${c}3")
    fixture.editor.document.setReadOnly(true)

    typeText(insertCommand)

    assertMode(Mode.NORMAL())
    assertState("12${c}3")
  }

  @Test
  fun `test cannot change text in read-only file with c motion`() {
    configureByText("12${c}3")
    fixture.editor.document.setReadOnly(true)

    // Try to change with 'cw' - should prevent the change
    typeText("cw")

    // Should remain in normal mode
    assertMode(Mode.NORMAL())
    assertState("12${c}3")
  }

  @Test
  fun `test cannot change text in read-only file with r motion`() {
    configureByText("12${c}3")
    fixture.editor.document.setReadOnly(true)

    // Try to change with 'r' - should prevent the change
    typeText("rX")

    // Should remain in normal mode
    assertMode(Mode.NORMAL())
    assertState("12${c}3")

  }
}
