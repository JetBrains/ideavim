/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.jetbrains.plugins.ideavim.action.change.insert

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.VimTestCase

class InsertDeletePreviousWordActionTest : VimTestCase() {
    // VIM-1655
    fun `test deleted word is not yanked`() {
        doTest(parseKeys("yiw", "3wea", "<C-W>", "<ESC>p"), """
            A Discovery

            I found <caret>it in a legendary land
        """.trimIndent(), """
            A Discovery

            I found it in a i<caret>t land
        """.trimIndent(), CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    }
}