/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.ex

import org.junit.jupiter.api.Test

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

  // TODO: Verify this is word and not WORD
}
