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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package org.jetbrains.plugins.ideavim.action.change.delete

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.group.visual.IdeaSelectionControl
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.waitAndAssertMode

/**
 * @author Alex Plate
 */
class DeleteVisualActionTest : VimTestCase() {
  fun `test delete block SE direction`() {
    val keys = parseKeys("<C-V>e2j", "d")
    val before = """
            A Discovery

            I |${c}found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    val after = """
            A Discovery

            I |$c| it in a legendary land
            al||ks and lavender and tufted grass,
            wh||t was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun `test delete block SW direction`() {
    val keys = parseKeys("<C-V>b2j", "d")
    val before = """
            A Discovery

            I |foun${c}d| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    val after = """
            A Discovery

            I |$c| it in a legendary land
            al||ks and lavender and tufted grass,
            wh||t was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun `test delete block NW direction`() {
    val keys = parseKeys("<C-V>b2k", "d")
    val before = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere ${c}i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    val after = """
            A Discovery

            I |$c| it in a legendary land
            al||ks and lavender and tufted grass,
            wh||t was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun `test delete block NE direction`() {
    val keys = parseKeys("<C-V>2e2k", "d")
    val before = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|${c}ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    val after = """
            A Discovery

            I |$c| it in a legendary land
            al||ks and lavender and tufted grass,
            wh||t was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun `test delete after extend selection`() {
    // This test emulates deletion after structural selection
    // In short, when caret is not on the selection end
    configureByText("""
            A Discovery

            ${s}I found it in a legendary land
            all rocks ${c}and lavender and tufted grass,
            where it was settled on some sodden sand
            ${se}hard by the torrent of a mountain pass.
        """.trimIndent())
    IdeaSelectionControl.controlNonVimSelectionChange(myFixture.editor)
    waitAndAssertMode(myFixture, CommandState.Mode.VISUAL)
    typeText(parseKeys("d"))
    myFixture.checkResult("""
            A Discovery

            hard by the torrent of a mountain pass.
        """.trimIndent())
    assertState(CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun `test delete with dollar motion`() {
    val keys = parseKeys("<C-V>3j$", "d")
    val before = """
            A Discovery

            I |${c}found it in a legendary land
            al|l rocks and lavender and tufted grass,[ additional symbols]
            wh|ere it was settled on some sodden sand
            ha|rd by the torrent of a mountain pass.
        """.trimIndent()
    val after = """
            A Discovery

            I |
            al|
            wh|
            ha|
        """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }
}
