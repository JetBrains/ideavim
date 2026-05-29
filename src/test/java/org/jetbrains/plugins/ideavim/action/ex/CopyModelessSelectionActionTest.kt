/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.ex

import com.maddyhome.idea.vim.register.RegisterConstants
import org.junit.jupiter.api.Test

class CopyModelessSelectionActionTest : VimExTestCase() {
  @Test
  fun `test CTRL-Y copies modeless selection to clipboard`() {
    typeText(":echo 'Hello world'")
    exEntryPanel.setModelessSelection(6, 11)
    typeText("<C-Y>")
    assertRegister(RegisterConstants.CLIPBOARD_REGISTER, "Hello")
  }

  @Test
  fun `test CTRL-Y does not update clipboard if no modeless selection`() {
    typeText(":echo 'Hello world'")
    typeText("<C-Y>")
    assertRegister(RegisterConstants.CLIPBOARD_REGISTER, null)
  }
}
