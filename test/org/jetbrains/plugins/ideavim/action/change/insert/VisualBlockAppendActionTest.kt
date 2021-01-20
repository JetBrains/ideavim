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
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class VisualBlockAppendActionTest : VimTestCase() {
  fun `test visual block append`() {
    val before = """
            ${c}int a;
            int b;
            int c;
            """.trimIndent()
    typeTextInFile(parseKeys("<C-V>", "2j", "e", "A", " const", "<Esc>"), before)
    val after = """
            int const a;
            int const b;
            int const c;
            """.trimIndent()
    myFixture.checkResult(after)
  }

  fun `test visual block append with dollar motion`() {
    val before = """
            ${c}int a;
            private String b;
            int c;
            """.trimIndent()
    typeTextInFile(parseKeys("<C-V>", "2j", "$", "A", " // My variables", "<Esc>"), before)
    val after = """
            int a; // My variables
            private String b; // My variables
            int c; // My variables
            """.trimIndent()
    myFixture.checkResult(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.MULTICARET)
  fun `test append in non block mode`() {
    doTest("vwAHello<esc>",
      """
                ${c}A Discovery

                ${c}I found it in a legendary land
                all rocks and ${c}lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
      """
                A DiscoveryHell${c}o

                I found it in a legendary landHell${c}o
                all rocks and lavender and tufted grass,Hell${c}o
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent(),
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE)
    assertMode(CommandState.Mode.COMMAND)
  }
}
