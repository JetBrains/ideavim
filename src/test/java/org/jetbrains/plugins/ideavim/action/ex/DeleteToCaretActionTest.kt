/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.ex

import org.junit.jupiter.api.Test

class DeleteToCaretActionTest : VimExTestCase() {
  @Test
  fun `test delete to start of line`() {
    typeText(":set incsearch<C-U>")
    assertExText("")

    deactivateExEntry()

    typeText(":set incsearch<Left><Left><Left><C-U>")
    assertExText("rch")
  }
}
