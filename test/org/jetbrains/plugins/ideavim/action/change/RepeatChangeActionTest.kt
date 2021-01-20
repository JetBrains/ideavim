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

package org.jetbrains.plugins.ideavim.action.change

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class RepeatChangeActionTest : VimTestCase() {
  fun `test simple repeat`() {
    val keys = listOf("v2erXj^", ".")
    val before = """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent()
    val after = """
                A Discovery

                XXXXXXXXXX in a legendary land
                ${c}XXXXXXXXXXand lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun `test simple repeat with dollar motion`() {
    val keys = listOf("v\$rXj^", ".")
    val before = """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent()
    val after = """
                A Discovery

                XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
                ${c}XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun `test repeat to line end`() {
    val keys = listOf("v2erXj\$b", ".")
    val before = """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent()
    val after = """
                A Discovery

                XXXXXXXXXX in a legendary land
                all rocks and lavender and tufted ${c}XXXXXX
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @VimBehaviorDiffers(description = "Different caret position")
  fun `test repeat multiline`() {
    val keys = listOf("vjlrXj", ".")
    val before = """
                A Discovery

                I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent()
    val after = """
                A Discovery

                I XXXXXXXXXXXXXXXXXXXXXXXXXXXX
                XXXXrocks and lavender and tufted grass,
                whe${c}XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
                XXXX by the torrent of a mountain pass.
                    """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun `test count doesn't affect repeat`() {
    val keys = listOf("v2erXj^", "10.")
    val before = """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent()
    val after = """
                A Discovery

                XXXXXXXXXX in a legendary land
                ${c}XXXXXXXXXXand lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.MULTICARET)
  fun `test multicaret`() {
    val keys = listOf("v2erXj^", ".")
    val before = """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where ${c}it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent()
    val after = """
                A Discovery

                XXXXXXXXXX in a legendary land
                ${c}XXXXXXXXXXand lavender and tufted grass,
                where XXXXXX settled on some sodden sand
                ${c}XXXXXXy the torrent of a mountain pass.
                    """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun `test line motion`() {
    val keys = listOf("VrXj^", ".")
    val before = """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent()
    val after = """
                A Discovery

                XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
                ${c}XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                    """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @VimBehaviorDiffers(description = "Wrong caret position")
  fun `test line motion to end`() {
    val keys = listOf("VjrX2j^", ".")
    val before = """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.""".trimIndent()
    val after = """
                A Discovery

                XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
                XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
                where it was settled on some sodden sand
                ${c}XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX""".trimIndent()
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @VimBehaviorDiffers(description = "Wrong caret position")
  fun `test line motion shift`() {
    val keys = listOf("V3j<", ".")
    val before = """
                |A Discovery
                |
                |        ${c}I found it in a legendary land
                |        all rocks and lavender and tufted grass,
                |        where it was settled on some sodden sand
                |        hard by the torrent of a mountain pass.
                """.trimMargin()
    val after = """
                |A Discovery
                |
                |${c}I found it in a legendary land
                |all rocks and lavender and tufted grass,
                |where it was settled on some sodden sand
                |hard by the torrent of a mountain pass.
                """.trimMargin()
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @VimBehaviorDiffers(description = "Wrong caret position")
  fun `test block motion`() {
    val keys = listOf("<C-V>jerXll", ".")
    val before = """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.
                """.trimIndent()
    val after = """
                A Discovery

                XXXound it in a legendary land
                XXX ${c}XXXks and lavender and tufted grass,
                wherXXXt was settled on some sodden sand
                hard by the torrent of a mountain pass.
                """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @VimBehaviorDiffers("""
                A Discovery

                XXXXXnd it in a legendary land
                XXXXXocks and lavender and tufted grass,
                XXXXX it was settled on some sodden sand
                hard ${c}XXXXXe torrent of a mountain pass.

    """)
  fun `test block motion to end`() {
    val keys = listOf("<C-V>jjerXjl", ".")
    val before = """
                A Discovery

                ${c}I found it in a legendary land
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand
                hard by the torrent of a mountain pass.

                """.trimIndent()
    val after = """
                A Discovery

                XXXXXnd it in a legendary land
                XXXXXocks and lavender and tufted grass,
                XXXXX it was settled on some sodden sand
                XXXXX${c}Xy the torrent of a mountain pass.

                """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @TestWithoutNeovim(SkipNeovimReason.UNCLEAR)
  fun `test block with dollar motion`() {
    val keys = listOf("<C-V>j\$rXj^", ".")
    val before = """
                A Discovery

                ${c}I found it in a legendary land[additional characters]
                all rocks and lavender and tufted grass,
                where it was settled on some sodden sand[additional characters]
                hard by the torrent of a mountain pass.
                """.trimIndent()
    val after = """
                A Discovery

                XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
                XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
                ${c}XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
                XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
                """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }
}
