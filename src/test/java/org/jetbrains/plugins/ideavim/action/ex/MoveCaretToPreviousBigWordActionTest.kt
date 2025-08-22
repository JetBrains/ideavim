/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.ex

import org.junit.jupiter.api.Test

class MoveCaretToPreviousBigWordActionTest : VimExTestCase() {
  @Test
  fun `test Shift-Left moves caret to start of current WORD`() {
    typeText(":set incsearch<S-Left>")
    assertExText("set ${c}incsearch")
  }

  @Test
  fun `test Shift-Left moves caret to start of command line`() {
    typeText(":set incsearch<S-Left><S-Left>")
    assertExText("${c}set incsearch")
  }

  @Test
  fun `test multiple Shift-Left moves caret to previous whitespace`() {
    typeText(":set     incsearch<S-Left>")
    assertExText("set     ${c}incsearch")

    typeText("<S-Left><S-Left>")
    assertExText("set   $c  incsearch")
  }

  @Test
  fun `test Shift-Left at start of text does nothing`() {
    typeText(":set incsearch<C-B>")
    assertExOffset(0)
    typeText("<S-Left>")
    assertExOffset(0)
  }

  @Test
  fun `test Ctrl-Left moves caret to start of current WORD`() {
    typeText(":set incsearch<C-Left>")
    assertExText("set ${c}incsearch")
  }

  @Test
  fun `test Ctrl-Left moves caret to start of command line`() {
    typeText(":set incsearch<C-Left><C-Left>")
    assertExText("${c}set incsearch")
  }

  @Test
  fun `test multiple Ctrl-Left moves caret to previous whitespace`() {
    typeText(":set     incsearch<C-Left>")
    assertExText("set     ${c}incsearch")

    typeText("<C-Left><C-Left>")
    assertExText("set   $c  incsearch")
  }

  @Test
  fun `test Ctrl-Left at start of text does nothing`() {
    typeText(":set incsearch<C-B>")
    assertExOffset(0)
    typeText("<C-Left>")
    assertExOffset(0)
  }
}
