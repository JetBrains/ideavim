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

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.StringHelper
import junit.framework.TestCase
import org.jetbrains.plugins.ideavim.VimTestCase

class ResetModeActionTest : VimTestCase() {
  fun `test reset from normal mode`() {
    val keys = StringHelper.parseKeys("<C-\\><C-N>")
    val before = "A Discovery"
    val after = "A Discovery"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    TestCase.assertFalse(myFixture.editor.selectionModel.hasSelection())
  }

  fun `test reset from insert mode`() {
    val keys = StringHelper.parseKeys("i", "<C-\\><C-N>")
    val before = "A Discovery"
    val after = "A Discovery"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    TestCase.assertFalse(myFixture.editor.selectionModel.hasSelection())
  }

  fun `test reset from visual mode`() {
    val keys = StringHelper.parseKeys("V", "<C-\\><C-N>")
    val before = "A Discovery"
    val after = "A Discovery"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    TestCase.assertFalse(myFixture.editor.selectionModel.hasSelection())
  }

  fun `test reset from select mode`() {
    val keys = StringHelper.parseKeys("gH", "<C-\\><C-N>")
    val before = "A Discovery"
    val after = "A Discovery"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    TestCase.assertFalse(myFixture.editor.selectionModel.hasSelection())
  }

  fun `test reset from operator-pending mode`() {
    val keys = StringHelper.parseKeys("d", "<C-\\><C-N>")
    val before = "A Discovery"
    val after = "A Discovery"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    TestCase.assertFalse(myFixture.editor.selectionModel.hasSelection())
  }

  fun `test reset from operator-pending mode with delete`() {
    val keys = StringHelper.parseKeys("d<Esc>dw")
    val before = "A Discovery"
    val after = "Discovery"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    TestCase.assertFalse(myFixture.editor.selectionModel.hasSelection())
  }
}
