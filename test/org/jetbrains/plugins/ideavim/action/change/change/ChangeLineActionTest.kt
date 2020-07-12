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

package org.jetbrains.plugins.ideavim.action.change.change

import com.maddyhome.idea.vim.command.CommandState
import org.jetbrains.plugins.ideavim.VimTestCase

class ChangeLineActionTest : VimTestCase() {
  fun `test on empty file`() {
    doTest("cc", "", "", CommandState.Mode.INSERT, CommandState.SubMode.NONE)
  }

  fun `test on empty file with S`() {
    doTest("S", "", "", CommandState.Mode.INSERT, CommandState.SubMode.NONE)
  }

  fun `test on last line with S`() {
    doTest("S", """
            I found it in a legendary land
            all ${c}rocks and lavender and tufted grass,
    """.trimIndent(), """
            I found it in a legendary land
            $c
    """.trimIndent(), CommandState.Mode.INSERT, CommandState.SubMode.NONE)
  }

  fun `test on last line with new line with S`() {
    doTest("S", """
            I found it in a legendary land
            all ${c}rocks and lavender and tufted grass,
            
    """.trimIndent(), """
            I found it in a legendary land
            $c
            
    """.trimIndent(), CommandState.Mode.INSERT, CommandState.SubMode.NONE)
  }

  fun `test on very last line with new line with S`() {
    doTest("S", """
            I found it in a legendary land
            all ${c}rocks and lavender and tufted grass,
    """.trimIndent(), """
            I found it in a legendary land
            $c
    """.trimIndent(), CommandState.Mode.INSERT, CommandState.SubMode.NONE)
  }

  fun `test on first line with new line with S`() {
    doTest("S", """
            I ${c}found it in a legendary land
            all rocks and lavender and tufted grass,
    """.trimIndent(), """
            $c
            all rocks and lavender and tufted grass,
    """.trimIndent(), CommandState.Mode.INSERT, CommandState.SubMode.NONE)
  }

  fun `test on last line with new line with cc`() {
    doTest("cc", """
            I found it in a legendary land
            all ${c}rocks and lavender and tufted grass,
            
    """.trimIndent(), """
            I found it in a legendary land
            $c
            
    """.trimIndent(), CommandState.Mode.INSERT, CommandState.SubMode.NONE)
  }

  fun `test on last line`() {
    doTest("cc", """
            I found it in a legendary land
            all rocks and lavender and tufted grass,
            $c
    """.trimIndent(), """
            I found it in a legendary land
            all rocks and lavender and tufted grass,
            $c
    """.trimIndent(), CommandState.Mode.INSERT, CommandState.SubMode.NONE)
  }
}
