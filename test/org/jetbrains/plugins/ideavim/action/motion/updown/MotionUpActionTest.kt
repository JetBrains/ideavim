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

package org.jetbrains.plugins.ideavim.action.motion.updown

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.StringHelper
import com.maddyhome.idea.vim.helper.vimLastColumn
import org.jetbrains.plugins.ideavim.VimTestCase

class MotionUpActionTest : VimTestCase() {
  fun `test last column empty`() {
    val keys = "k"
    val before = """
            I found it in a legendary land
            all rocks and lave${c}nder and tufted grass,
        """.trimIndent()
    val after = """
            I found it in a le${c}gendary land
            all rocks and lavender and tufted grass,
        """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun `test last column is incorrect`() {
    val keys = StringHelper.parseKeys("k")
    val before = """
            I found it in a legendary land
            all rocks and lave${c}nder and tufted grass,
        """.trimIndent()
    val after = """
            I found it in a le${c}gendary land
            all rocks and lavender and tufted grass,
        """.trimIndent()
    doTestWithoutNeovim(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE) {
      it.caretModel.primaryCaret.vimLastColumn = 5
    }
  }

  fun `test last column to shorter line`() {
    val keys = "kkkjjj"
    val before = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted ${c}grass,
        """.trimIndent()
    val after = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted ${c}grass,
        """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun `test last column wrong lastColumn`() {
    val keys = StringHelper.parseKeys("k")
    val before = """
            I found it in a legendary land
            all rocks and lavender and tufted ${c}grass,
        """.trimIndent()
    val after = """
            I found it in a legendary lan${c}d
            all rocks and lavender and tufted grass,
        """.trimIndent()
    doTestWithoutNeovim(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE) {
      it.caretModel.primaryCaret.vimLastColumn = 0
    }
  }
}
