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

package org.jetbrains.plugins.ideavim.action.copy

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.helper.VimBehaviourDiffers
import org.jetbrains.plugins.ideavim.VimTestCase

class PutTextAfterCursorActionTest : VimTestCase() {
  @VimBehaviourDiffers(originalVimAfter = """
        |Same length text 1
        |Same length text 2
        |Same length text 3
        |${c}Same length text 1
        |Same length text 2
  """)
  fun `test copy linear text using clipboard`() {
    doTest(parseKeys("Vj\"*y", "G\"*p"),
      """${c}Same length text 1
        |Same length text 2
        |Same length text 3
      """.trimMargin(),
      """Same length text 1
        |Same length text 2
        |Same length text 3
        |${c}Same length text 1
        |Same length text 2
        |
      """.trimMargin(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }
}