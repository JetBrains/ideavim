/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action

import com.maddyhome.idea.vim.action.VimShortcutKeyAction
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.KeyStroke
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VimShortcutKeyActionTest : VimTestCase() {

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `plain Tab is a Vim-only editor key`() {
    val tab = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0)
    assertTrue(VimShortcutKeyAction.VIM_ONLY_EDITOR_KEYS.contains(tab))
  }

  @TestWithoutNeovim(SkipNeovimReason.NOT_VIM_TESTING)
  @Test
  fun `S-Tab is not a Vim-only editor key so sethandler can release it to the IDE`() {
    val shiftTab = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK)
    assertFalse(VimShortcutKeyAction.VIM_ONLY_EDITOR_KEYS.contains(shiftTab))
  }
}
