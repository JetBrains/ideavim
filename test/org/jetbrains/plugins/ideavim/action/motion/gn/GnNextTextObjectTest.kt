/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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

@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.action.motion.gn

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.Direction
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import javax.swing.KeyStroke

class GnNextTextObjectTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  fun `test delete word`() {
    doTestWithSearch(
      parseKeys("dgn"),
      """
      Hello, ${c}this is a test here
      """.trimIndent(),
      """
        Hello, this is a ${c} here
      """.trimIndent()
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  fun `test delete second word`() {
    doTestWithSearch(
      parseKeys("2dgn"),
      """
      Hello, ${c}this is a test here
      Hello, this is a test here
      """.trimIndent(),
      """
        Hello, this is a test here
        Hello, this is a ${c} here
      """.trimIndent()
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  fun `test with repeat`() {
    doTestWithSearch(
      parseKeys("cgnNewValue<ESC>..."),
      """
      Hello, ${c}this is a test here
      Hello, this is a test here
      Hello, this is a test here
      Hello, this is a test here
      Hello, this is a test here
      """.trimIndent(),
      """
      Hello, this is a NewValue here
      Hello, this is a NewValue here
      Hello, this is a NewValue here
      Hello, this is a NewValu${c}e here
      Hello, this is a test here
      """.trimIndent()
    )
  }

  fun `test gn uses last used pattern not just search pattern`() {
    doTest(
      listOf("/is<CR>", ":s/test/tester/<CR>", "0", "dgn"),
      "Hello, ${c}this is a test here",
      "Hello, this is a ${c}er here",
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE
    )
  }

  private fun doTestWithSearch(keys: List<KeyStroke>, before: String, after: String) {
      configureByText(before)
      VimPlugin.getSearch().setLastSearchState(myFixture.editor, "test", "", Direction.FORWARDS)
      typeText(keys)
      assertState(after)
      assertState(CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }
}
