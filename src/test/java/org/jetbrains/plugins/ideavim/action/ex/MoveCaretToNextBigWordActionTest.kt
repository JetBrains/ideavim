/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.ex

import org.junit.jupiter.api.Test

class MoveCaretToNextBigWordActionTest : VimExTestCase() {
  @Test
  fun `test Shift-Right moves caret to end of current WORD`() {
    typeText(":set incsearch<C-B>")
    assertExText("${c}set incsearch")

    typeText("<S-Right>")
    assertExText("set$c incsearch")
  }

  @Test
  fun `test Shift-Right moves caret to end of command line`() {
    typeText(":set incsearch<C-B>")
    assertExText("${c}set incsearch")

    typeText("<S-Right><S-Right>")
    assertExText("set incsearch$c")
  }

  @Test
  fun `test multiple Shift-Right moves caret to next whitespace`() {
    typeText(":set     incsearch<C-B>")
    assertExText("${c}set     incsearch")

    // Command-line doesn't really use WORD, it just moves to the next whitespace character.
    // First moves to whitespace after `set`, then next whitespace, then third whitespace
    typeText("<S-Right><S-Right><S-Right>")
    assertExText("set  $c   incsearch")
  }

  @Test
  fun `test Shift-Right at end of text does nothing`() {
    typeText(":set incsearch<S-Right>")
    assertExText("set incsearch$c")
  }

  @Test
  fun `test Ctrl-Right moves caret to end of current WORD`() {
    typeText(":set incsearch<C-B>")
    assertExText("${c}set incsearch")

    typeText("<C-Right>")
    assertExText("set$c incsearch")
  }

  @Test
  fun `test Ctrl-Right moves caret to end of command line`() {
    typeText(":set incsearch<C-B>")
    assertExText("${c}set incsearch")

    typeText("<C-Right><C-Right>")
    assertExText("set incsearch$c")
  }

  @Test
  fun `test multiple Ctrl-Right moves caret to next whitespace`() {
    typeText(":set     incsearch<C-B>")
    assertExText("${c}set     incsearch")

    // Command-line doesn't really use WORD, it just moves to the next whitespace character.
    // First moves to whitespace after `set`, then next whitespace, then third whitespace
    typeText("<C-Right><C-Right><C-Right>")
    assertExText("set  $c   incsearch")
  }

  @Test
  fun `test Ctrl-Right at end of text does nothing`() {
    typeText(":set incsearch<C-Right>")
    assertExText("set incsearch$c")
  }
}
