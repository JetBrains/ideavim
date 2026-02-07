/*
 * Copyright 2003-2026 The IdeaVim authors
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
  fun `test read-only file allows insert entry but blocks changes`(insertCommand: String) {
    configureByText("12${c}3")
    fixture.editor.document.setReadOnly(true)

    // Enter insert-like commands, then immediately escape; no text should change and ESC should work
    typeText("$insertCommand<Esc>")

    assertMode(Mode.NORMAL())
    assertState("12${c}3")
  }

  @Test
  fun `esc with insert should check if file is writable`() {
    configureByText("12${c}3")
    typeText("10i1")
    fixture.editor.document.setReadOnly(true)

    // escape trigger write 1 10 times
    typeText("<Esc>")

    // Should remain in normal mode
    assertMode(Mode.NORMAL())
    assertState("121${c}3")
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

  @Test
  fun `test exit insert mode in read-only file`() {
    // Test that ESC works in read-only files without hanging
    configureByText("test ${c}content")
    fixture.editor.document.setReadOnly(true)

    // This should complete without hanging or errors
    typeText("i<Esc>")

    // Reset read-only status
    fixture.editor.document.setReadOnly(false)
  }

  @Test
  fun `test exit insert mode with repeat count in read-only file`() {
    // Test that ESC works with repeat counts in read-only files
    configureByText("${c}hello")
    fixture.editor.document.setReadOnly(true)

    // 3i should repeat the insert 3 times when ESC is pressed
    // In read-only files, this should exit cleanly without hanging
    typeText("3i<Esc>")
    assertMode(Mode.NORMAL())
    assertState("${c}hello")

    // Reset read-only status
    fixture.editor.document.setReadOnly(false)
  }
}
