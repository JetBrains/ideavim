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

package org.jetbrains.plugins.ideavim.action.change.insert

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase

class InsertDeleteInsertedTextActionTest : VimTestCase() {
  // VIM-1655
  fun `test deleted text is not yanked`() {
    doTest(listOf("yiw", "ea", "Hello", "<C-U>", "<ESC>p"), """
            A Discovery

            I found ${c}it in a legendary land
        """.trimIndent(), """
            A Discovery

            I found iti${c}t in a legendary land
        """.trimIndent(), CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  // VIM-1655
  @VimBehaviorDiffers(description = "Inserted text is not deleted after <C-U>")
  fun `test deleted text is not yanked after replace`() {
    doTest(listOf("yiw", "eR", "Hello", "<C-U>", "<ESC>p"), """
            A Discovery

            I found ${c}it in a legendary land
        """.trimIndent(), """
            A Discovery

            I found ii${c}ta legendary land
        """.trimIndent(), CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }
}
