/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.key.MappingOwner
import junit.framework.TestCase
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class ResetModeActionTest : VimTestCase() {
  private val owner = MappingOwner.Plugin.get("ResetModeActionTest")

  fun `test reset from normal mode`() {
    val keys = "<C-\\><C-N>"
    val before = "A Discovery"
    val after = "A Discovery"
    doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
    TestCase.assertFalse(myFixture.editor.selectionModel.hasSelection())
  }

  fun `test reset from insert mode`() {
    val keys = listOf("i", "<C-\\><C-N>")
    val before = "A Discovery"
    val after = "A Discovery"
    doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
    TestCase.assertFalse(myFixture.editor.selectionModel.hasSelection())
  }

  fun `test reset from insert mode check position`() {
    val keys = listOf("i", "<C-\\><C-N>")
    val before = "A Disc${c}overy"
    val after = "A Dis${c}covery"
    doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
    TestCase.assertFalse(myFixture.editor.selectionModel.hasSelection())
  }

  fun `test reset and execute command`() {
    val keys = listOf("i", "<C-\\><C-N>", "3l")
    val before = "${c}A Discovery"
    val after = "A D${c}iscovery"
    doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
    TestCase.assertFalse(myFixture.editor.selectionModel.hasSelection())
  }

  fun `test reset from visual mode`() {
    val keys = listOf("V", "<C-\\><C-N>")
    val before = "A Discovery"
    val after = "A Discovery"
    doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
    TestCase.assertFalse(myFixture.editor.selectionModel.hasSelection())
  }

  fun `test reset from select mode`() {
    val keys = listOf("gH", "<C-\\><C-N>")
    val before = "A Discovery"
    val after = "A Discovery"
    doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
    TestCase.assertFalse(myFixture.editor.selectionModel.hasSelection())
  }

  fun `test reset from operator-pending mode`() {
    val keys = listOf("d", "<C-\\><C-N>")
    val before = "A Discovery"
    val after = "A Discovery"
    doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
    TestCase.assertFalse(myFixture.editor.selectionModel.hasSelection())
  }

  fun `test reset from operator-pending mode with delete`() {
    val keys = "d<Esc>dw"
    val before = "A Discovery"
    val after = "Discovery"
    doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
    TestCase.assertFalse(myFixture.editor.selectionModel.hasSelection())
  }

  fun `test delete command after resetting operator-pending mode`() {
    val keys = listOf("d", "<C-\\><C-N>", "dw")
    val before = "A Discovery"
    val after = "Discovery"
    doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
    TestCase.assertFalse(myFixture.editor.selectionModel.hasSelection())
  }

  fun `test delete command after resetting operator-pending mode with esc`() {
    val keys = listOf("d", "<Esc>", "dw")
    val before = "A Discovery"
    val after = "Discovery"
    doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
    TestCase.assertFalse(myFixture.editor.selectionModel.hasSelection())
  }

  @TestWithoutNeovim(SkipNeovimReason.CTRL_CODES)
  fun `test delete command after resetting operator-pending mode with ctrl open bracket`() {
    val keys = listOf("d", "<C-[>", "dw")
    val before = "A Discovery"
    val after = "Discovery"
    doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
    TestCase.assertFalse(myFixture.editor.selectionModel.hasSelection())
  }

  @TestWithoutNeovim(SkipNeovimReason.MAPPING)
  fun `test delete command after resetting operator-pending mode with mapping`() {
    VimPlugin.getKey()
      .putKeyMapping(MappingMode.NVO, injector.parser.parseKeys("<C-D>"), owner, injector.parser.parseKeys("<Esc>"), false)

    val keys = listOf("d", "<C-D>", "dw")
    val before = "A Discovery"
    val after = "Discovery"
    doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
    TestCase.assertFalse(myFixture.editor.selectionModel.hasSelection())
  }

  fun `test non-delete commands after resetting operator-pending mode`() {
    val keys = listOf("c", "<C-\\><C-N>", "another")
    val before = "A Discovery"
    val after = "Another Discovery"
    doTest(keys, before, after, VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE)
    TestCase.assertFalse(myFixture.editor.selectionModel.hasSelection())
  }

  fun `test delete after escaping t`() {
    val keys = "dt<esc>D"
    val before = "A ${c}Discovery"
    val after = "A "
    doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
    TestCase.assertFalse(myFixture.editor.selectionModel.hasSelection())
  }
}
