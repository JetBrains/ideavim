/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.state.mode.Mode
import org.junit.jupiter.api.Test

class RegisterActionsTest : VimTestCase() {
  @Test
  fun `test simple action`() {
    val before = "I ${c}found it in a legendary land"
    val after = "I f${c}ound it in a legendary land"
    doTest("l", before, after, Mode.NORMAL())
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.EDITOR_MODIFICATION)
  @Test
  fun `test action in disabled plugin`() {
    try {
      setupChecks {
        caretShape = false
      }
      val before = "I ${c}found it in a legendary land"
      val after = "I jklwB${c}found it in a legendary land"
      doTest("jklwB", before, after, Mode.NORMAL()) {
        VimPlugin.setEnabled(false)
      }
    } finally {
      VimPlugin.setEnabled(true)
    }
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.EDITOR_MODIFICATION)
  @Test
  fun `test turn plugin off and on`() {
    val before = "I ${c}found it in a legendary land"
    val after = "I f${c}ound it in a legendary land"
    doTest("l", before, after, Mode.NORMAL()) {
      VimPlugin.setEnabled(false)
      VimPlugin.setEnabled(true)
    }
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.EDITOR_MODIFICATION)
  @Test
  fun `test enable twice`() {
    val before = "I ${c}found it in a legendary land"
    val after = "I f${c}ound it in a legendary land"
    doTest("l", before, after, Mode.NORMAL()) {
      VimPlugin.setEnabled(false)
      VimPlugin.setEnabled(true)
      VimPlugin.setEnabled(true)
    }
  }
}
