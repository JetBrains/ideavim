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

package org.jetbrains.plugins.ideavim.action.motion.visual

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.option.OptionsManager
import org.jetbrains.plugins.ideavim.VimTestCase

class VisualToggleLineModeActionTest : VimTestCase() {
  fun `test enter visual with count`() {
    doTest("1V",
      """
                    A Discovery

                    I ${c}found it in a legendary land
                    all rocks and lavender and tufted grass,
                    where it was settled on some sodden sand
                    hard by the torrent of a mountain pass.
                """.trimIndent(),
      """
                    A Discovery

                    ${s}I ${c}found it in a legendary land
                    ${se}all rocks and lavender and tufted grass,
                    where it was settled on some sodden sand
                    hard by the torrent of a mountain pass.
                """.trimIndent(),
      CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_LINE)
  }

  fun `test enter visual with count multicaret`() {
    doTest("1V",
      """
                    A Discovery

                    I ${c}found it in a legendary land
                    all rocks and lavender and tufted grass,
                    where it ${c}was settled on some sodden sand
                    hard by the torrent of a mountain pass.
                """.trimIndent(),
      """
                    A Discovery

                    ${s}I ${c}found it in a legendary land
                    ${se}all rocks and lavender and tufted grass,
                    ${s}where it ${c}was settled on some sodden sand
                    ${se}hard by the torrent of a mountain pass.
                """.trimIndent(),
      CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_LINE)
  }

  fun `test enter visual with 3 count`() {
    doTest("3V",
      """
                    A Discovery

                    I ${c}found it in a legendary land
                    all rocks and lavender and tufted grass,
                    where it was settled on some sodden sand
                    hard by the torrent of a mountain pass.
                """.trimIndent(),
      """
                    A Discovery

                    ${s}I found it in a legendary land
                    all rocks and lavender and tufted grass,
                    wh${c}ere it was settled on some sodden sand
                    ${se}hard by the torrent of a mountain pass.
                """.trimIndent(),
      CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_LINE)
  }

  fun `test enter visual with 100 count`() {
    doTest("100V",
      """
                    A Discovery

                    I ${c}found it in a legendary land
                    all rocks and lavender and tufted grass,
                    where it was settled on some sodden sand
                    hard by the torrent of a mountain pass.
                """.trimIndent(),
      """
                    A Discovery

                    ${s}I found it in a legendary land
                    all rocks and lavender and tufted grass,
                    where it was settled on some sodden sand
                    ha${c}rd by the torrent of a mountain pass.${se}
                """.trimIndent(),
      CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_LINE)
  }

  fun `test selectmode option`() {
    configureByText("""
                    A Discovery

                    I${c} found it in a legendary land
                    all rocks and lavender and tufted grass,
                    where it was settled on some sodden sand[long line]
                    hard by the torrent of a mountain pass.
        """.trimIndent())
    OptionsManager.selectmode.set("cmd")
    typeText(parseKeys("V"))
    assertState(CommandState.Mode.SELECT, CommandState.SubMode.VISUAL_LINE)
  }
}
