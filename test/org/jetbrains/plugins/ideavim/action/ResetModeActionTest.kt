/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2020 The IdeaVim authors
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

package org.jetbrains.plugins.ideavim.action

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.key.MappingOwner
import junit.framework.TestCase
import org.jetbrains.plugins.ideavim.VimTestCase

class ResetModeActionTest : VimTestCase() {
  private val owner = MappingOwner.Plugin.get("ResetModeActionTest")

  fun `test reset from normal mode`() {
    val keys = parseKeys("<C-\\><C-N>")
    val before = "A Discovery"
    val after = "A Discovery"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    TestCase.assertFalse(myFixture.editor.selectionModel.hasSelection())
  }

  fun `test reset from insert mode`() {
    val keys = parseKeys("i", "<C-\\><C-N>")
    val before = "A Discovery"
    val after = "A Discovery"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    TestCase.assertFalse(myFixture.editor.selectionModel.hasSelection())
  }

  fun `test reset from visual mode`() {
    val keys = parseKeys("V", "<C-\\><C-N>")
    val before = "A Discovery"
    val after = "A Discovery"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    TestCase.assertFalse(myFixture.editor.selectionModel.hasSelection())
  }

  fun `test reset from select mode`() {
    val keys = parseKeys("gH", "<C-\\><C-N>")
    val before = "A Discovery"
    val after = "A Discovery"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    TestCase.assertFalse(myFixture.editor.selectionModel.hasSelection())
  }

  fun `test reset from operator-pending mode`() {
    val keys = parseKeys("d", "<C-\\><C-N>")
    val before = "A Discovery"
    val after = "A Discovery"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    TestCase.assertFalse(myFixture.editor.selectionModel.hasSelection())
  }

  fun `test reset from operator-pending mode with delete`() {
    val keys = parseKeys("d<Esc>dw")
    val before = "A Discovery"
    val after = "Discovery"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    TestCase.assertFalse(myFixture.editor.selectionModel.hasSelection())
  }

  fun `test delete command after resetting operator-pending mode`() {
    val keys = parseKeys("d", "<C-\\><C-N>", "dw")
    val before = "A Discovery"
    val after = "Discovery"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    TestCase.assertFalse(myFixture.editor.selectionModel.hasSelection())
  }

  fun `test delete command after resetting operator-pending mode with esc`() {
    val keys = parseKeys("d", "<Esc>", "dw")
    val before = "A Discovery"
    val after = "Discovery"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    TestCase.assertFalse(myFixture.editor.selectionModel.hasSelection())
  }

  fun `test delete command after resetting operator-pending mode with ctrl open bracket`() {
    val keys = parseKeys("d", "<C-[>", "dw")
    val before = "A Discovery"
    val after = "Discovery"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    TestCase.assertFalse(myFixture.editor.selectionModel.hasSelection())
  }

  fun `test delete command after resetting operator-pending mode with mapping`() {
    VimPlugin.getKey()
      .putKeyMapping(MappingMode.NVO, parseKeys("<C-D>"), owner, parseKeys("<Esc>"), false)

    val keys = parseKeys("d", "<C-D>", "dw")
    val before = "A Discovery"
    val after = "Discovery"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    TestCase.assertFalse(myFixture.editor.selectionModel.hasSelection())
  }

  fun `test non-delete commands after resetting operator-pending mode`() {
    val keys = parseKeys("c", "<C-\\><C-N>", "another")
    val before = "A Discovery"
    val after = "Another Discovery"
    doTest(keys, before, after, CommandState.Mode.INSERT, CommandState.SubMode.NONE)
    TestCase.assertFalse(myFixture.editor.selectionModel.hasSelection())
  }

  fun `test delete after escaping t`() {
    val keys = parseKeys("dt<esc>D")
    val before = "A ${c}Discovery"
    val after = "A "
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    TestCase.assertFalse(myFixture.editor.selectionModel.hasSelection())
  }
}
