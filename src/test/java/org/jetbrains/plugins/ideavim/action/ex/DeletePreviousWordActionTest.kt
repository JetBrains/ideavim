/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.ex

import org.junit.jupiter.api.Test

@Suppress("SpellCheckingInspection")
class DeletePreviousWordActionTest : VimExTestCase() {
  @Test
  fun `test delete word before caret`() {
    typeText(":set incsearch<C-W>")
    assertExText("set ")

    deactivateExEntry()

    typeText(":set incsearch<Left><Left><Left>")
    typeText("<C-W>")
    assertExText("set rch")
  }

  @Test
  fun `test delete up to previous word boundary`() {
    typeText(":set keymodel=continueselect,stopselect<C-W>")
    assertExText("set keymodel=continueselect,")
  }

  @Test
  fun `test delete up to previous word boundary 2`() {
    typeText(":set keymodel=continueselect,<C-W>")
    assertExText("set keymodel=continueselect")
  }

  @Test
  fun `test delete up to previous word boundary 3`() {
    typeText(":set keymodel=continueselect<C-W>")
    assertExText("set keymodel=")
  }

  @Test
  fun `test delete word honours iskeyword option`() {
    // This is the same as in help files (mostly. We seem to have problems with ^|)
//    enterCommand("set iskeyword=!-~,^*,^|,^\",192-255")
    enterCommand("set iskeyword=!-~,^*,^\",192-255")
    typeText(":set keymodel=continueselect,stopselect<C-W>")
    assertExText("set ")
  }
}
