/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package org.jetbrains.plugins.ideavim

import com.maddyhome.idea.vim.RegisterActions.VIM_ACTIONS_EP
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.handler.ActionBeanClass
import com.maddyhome.idea.vim.key.CommandNode
import com.maddyhome.idea.vim.key.CommandPartNode
import junit.framework.TestCase
import javax.swing.KeyStroke

class RegisterActionsTest : VimTestCase() {
  fun `test simple action`() {
    val before = "I ${c}found it in a legendary land"
    val after = "I f${c}ound it in a legendary land"
    doTest("l", before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.EDITOR_MODIFICATION)
  fun `test action in disabled plugin`() {
    try {
      setupChecks {
        caretShape = false
      }
      val before = "I ${c}found it in a legendary land"
      val after = "I jklwB${c}found it in a legendary land"
      doTest("jklwB", before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE) {
        VimPlugin.setEnabled(false)
      }
    } finally {
      VimPlugin.setEnabled(true)
    }
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.EDITOR_MODIFICATION)
  fun `test turn plugin off and on`() {
    val before = "I ${c}found it in a legendary land"
    val after = "I f${c}ound it in a legendary land"
    doTest("l", before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE) {
      VimPlugin.setEnabled(false)
      VimPlugin.setEnabled(true)
    }
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.EDITOR_MODIFICATION)
  fun `test enable twice`() {
    val before = "I ${c}found it in a legendary land"
    val after = "I f${c}ound it in a legendary land"
    doTest("l", before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE) {
      VimPlugin.setEnabled(false)
      VimPlugin.setEnabled(true)
      VimPlugin.setEnabled(true)
    }
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.EDITOR_MODIFICATION)
  fun `test unregister extension`() {
    val before = "I ${c}found it in a legendary land"
    val after = "I f${c}ound it in a legendary land"
    var motionRightAction: ActionBeanClass? = null
    doTest("l", before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE) {
      motionRightAction = VIM_ACTIONS_EP.extensions().filter { it.actionId == "VimPreviousTabAction" }.findFirst().get()

      assertNotNull(getCommandNode())

      @Suppress("DEPRECATION")
      VIM_ACTIONS_EP.getPoint(null).unregisterExtension(motionRightAction!!)
      assertNull(getCommandNode())
    }
    @Suppress("DEPRECATION")
    VIM_ACTIONS_EP.getPoint(null).registerExtension(motionRightAction!!)
    TestCase.assertNotNull(getCommandNode())
  }

  private fun getCommandNode(): CommandNode<ActionBeanClass>? {
    // TODO: 08.02.2020 Sorry if your tests will fail because of this test
    val node = VimPlugin.getKey().getKeyRoot(MappingMode.NORMAL)[KeyStroke.getKeyStroke('g')] as CommandPartNode
    return node[KeyStroke.getKeyStroke('T')] as CommandNode<ActionBeanClass>?
  }
}
