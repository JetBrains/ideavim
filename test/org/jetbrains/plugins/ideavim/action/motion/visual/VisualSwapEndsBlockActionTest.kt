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

import com.intellij.openapi.editor.LogicalPosition
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class VisualSwapEndsBlockActionTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.VISUAL_BLOCK_MODE)
  fun `test simple block selection SE`() {
    val keys = listOf("<C-V>2e2j", "O")
    val before = """
            A Discovery

            I ${c}|found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    val after = """
            A Discovery

            I ${s}${c}|found|${se} it in a legendary land
            al${s}${c}|l roc|${se}ks and lavender and tufted grass,
            wh${s}${c}|ere i|${se}t was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_BLOCK)
    assertEquals(LogicalPosition(4, 2), myFixture.editor.caretModel.primaryCaret.logicalPosition)
  }

  @TestWithoutNeovim(SkipNeovimReason.VISUAL_BLOCK_MODE)
  fun `test simple block selection SW`() {
    val keys = listOf("<C-V>2b2j", "O")
    val before = """
            A Discovery

            I |found${c}| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    val after = """
            A Discovery

            I ${s}|found${c}|${se} it in a legendary land
            al${s}|l roc${c}|${se}ks and lavender and tufted grass,
            wh${s}|ere i${c}|${se}t was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_BLOCK)
    assertEquals(LogicalPosition(4, 8), myFixture.editor.caretModel.primaryCaret.logicalPosition)
  }

  @TestWithoutNeovim(SkipNeovimReason.VISUAL_BLOCK_MODE)
  fun `test simple block selection NE`() {
    val keys = listOf("<C-V>3e2k", "O")
    val before = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh${c}|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    val after = """
            A Discovery

            I ${s}${c}|found|${se} it in a legendary land
            al${s}${c}|l roc|${se}ks and lavender and tufted grass,
            wh${s}${c}|ere i|${se}t was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_BLOCK)
    assertEquals(LogicalPosition(2, 2), myFixture.editor.caretModel.primaryCaret.logicalPosition)
  }

  @TestWithoutNeovim(SkipNeovimReason.VISUAL_BLOCK_MODE)
  fun `test simple block selection NW`() {
    val keys = listOf("<C-V>3b2k", "O")
    val before = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i${c}|t was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    val after = """
            A Discovery

            I ${s}|found${c}|${se} it in a legendary land
            al${s}|l roc${c}|${se}ks and lavender and tufted grass,
            wh${s}|ere i${c}|${se}t was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_BLOCK)
    assertEquals(LogicalPosition(2, 8), myFixture.editor.caretModel.primaryCaret.logicalPosition)
  }

  @TestWithoutNeovim(SkipNeovimReason.VISUAL_BLOCK_MODE)
  fun `test with short line`() {
    val keys = listOf("<C-V>2j5e", "O")
    val before = """
            A Discovery

            I found it in a legendary ${c}land{here you can see}
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand{some new symbols}
            hard by the torrent of a mountain pass.
        """.trimIndent()
    val after = """
            A Discovery

            I found it in a legendary ${s}${c}land{here you can s${se}ee}
            all rocks and lavender and${s}${c} tufted grass,${se}
            where it was settled on so${s}${c}me sodden sand{some${se} new symbols}
            hard by the torrent of a mountain pass.
        """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_BLOCK)
    assertEquals(LogicalPosition(4, 26), myFixture.editor.caretModel.primaryCaret.logicalPosition)
  }

  @TestWithoutNeovim(SkipNeovimReason.VISUAL_BLOCK_MODE)
  fun `test to long line`() {
    val keys = listOf("<C-V>j5e", "O")
    val before = """
            A Discovery

            I found it in a legendary ${c}land
            all rocks and lavender and tufted grass,[additional characters]
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    val after = """
            A Discovery

            I found it in a legendary ${s}${c}land${se}
            all rocks and lavender and${s}${c} tuf${se}ted grass,[additional characters]
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_BLOCK)
    assertEquals(LogicalPosition(3, 26), myFixture.editor.caretModel.primaryCaret.logicalPosition)
  }


  fun testVisualSwapEndsBlockActionInBlockMode() {
    typeTextInFile(parseKeys("<C-V>", "2l", "j", "O"),
      """
                    a${c}abcc
                    ddeff
                    
                    """.trimIndent())
    myFixture.checkResult("""
    a${s}${c}abc${se}c
    d${s}${c}def${se}f
    
    """.trimIndent())
  }

  fun testVisualBlockMovementAfterSwapEndsBlockAction() {
    typeTextInFile(parseKeys("<C-V>", "2l", "j", "O", "k", "h", "j"),
      """
                    aabcc
                    d${c}deff
                    gghii
                    jjkll

                    """.trimIndent())
    myFixture.checkResult(("""
    aabcc
    ${s}${c}ddef${se}f
    ${s}${c}gghi${se}i
    jjkll

    """.trimIndent()))
    typeText(parseKeys("j"))
    myFixture.checkResult(("""
    aabcc
    ${s}${c}ddef${se}f
    ${s}${c}gghi${se}i
    ${s}${c}jjkl${se}l

    """.trimIndent()))
  }
}
