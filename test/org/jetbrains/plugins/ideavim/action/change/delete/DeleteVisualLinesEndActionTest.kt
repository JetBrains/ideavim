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

@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.action.change.delete

import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.helper.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase

class DeleteVisualLinesEndActionTest : VimTestCase() {
  fun `test simple deletion`() {
    val keys = listOf("v", "D")
    val before = """
            A Discovery

            I ${c}found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    val after = """
            A Discovery

            ${c}all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @VimBehaviorDiffers(originalVimAfter = """
            A Discovery

                ${c}all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """)
  fun `test simple deletion with indent`() {
    val keys = listOf("v", "D")
    val before = """
            A Discovery

            I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    val after = """
            A Discovery

            ${c}    all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun `test simple deletion empty line`() {
    val keys = listOf("v", "D")
    val before = """
            A Discovery
            ${c}
            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    val after = """
            A Discovery
            ${c}I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @VimBehaviorDiffers("""
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            ${c}
    """)
  fun `test simple deletion last line`() {
    val keys = listOf("v", "D")
    val before = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the ${c}torrent of a mountain pass.

        """.trimIndent()
    val after = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden san${c}d

        """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun `test simple deletion first line`() {
    val keys = listOf("v", "D")
    val before = """
            A ${c}Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    val after = """
            ${c}
            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun `test simple deletion before empty`() {
    val keys = listOf("v", "D")
    val before = """
            A Discovery

            I found it in a legendary land
            all ${c}rocks and lavender and tufted grass,

            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    val after = """
            A Discovery

            I found it in a legendary land
            ${c}
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @VimBehaviorDiffers("""
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            ${c}where it was settled on some sodden sand"""
  )
  fun `test simple deletion last line without empty line`() {
    val keys = listOf("v", "D")
    val before = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the ${c}torrent of a mountain pass.""".trimIndent()
    val after = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden san${c}d

            """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun `test simple deletion multiline`() {
    val keys = listOf("vj", "D")
    val before = """
            A Discovery

            I ${c}found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
            """.trimIndent()
    val after = """
            A Discovery

            ${c}where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
            """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun `test simple deletion multiline motion up`() {
    val keys = listOf("vk", "D")
    val before = """
            A Discovery

            I found it in a legendary land
            all ${c}rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
            """.trimIndent()
    val after = """
            A Discovery

            ${c}where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
            """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun `test delete visual lines end action`() {
    typeTextInFile(parseKeys("v", "2j", "D"),
      """
                    a${c}bcde
                    abcde
                    abcde
                    abcde
                    abcd${c}e
                    abcde
                    abcde

                    """.trimIndent())
    myFixture.checkResult("${c}abcd${c}e\n")
  }

  fun `test line simple deletion`() {
    val keys = listOf("V", "D")
    val before = """
            A Discovery

            I ${c}found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    val after = """
            A Discovery

            ${c}all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @VimBehaviorDiffers(originalVimAfter = """
            A Discovery

                ${c}all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """)
  fun `test line deletion with indent`() {
    val keys = listOf("V", "D")
    val before = """
            A Discovery

            I ${c}found it in a legendary land
                all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    val after = """
            A Discovery

            ${c}    all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun `test line deletion empty line`() {
    val keys = listOf("V", "D")
    val before = """
            A Discovery
            ${c}
            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    val after = """
            A Discovery
            ${c}I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @VimBehaviorDiffers("""
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            ${c}
    """)
  fun `test line deletion last line`() {
    val keys = listOf("V", "D")
    val before = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the ${c}torrent of a mountain pass.

        """.trimIndent()
    val after = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden san${c}d

        """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  @VimBehaviorDiffers("""
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            ${c}where it was settled on some sodden sand"""
  )
  fun `test line deletion last line without empty line`() {
    val keys = listOf("V", "D")
    val before = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the ${c}torrent of a mountain pass.""".trimIndent()
    val after = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand

            """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun `test line deletion multiline`() {
    val keys = listOf("Vj", "D")
    val before = """
            A Discovery

            I ${c}found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
            """.trimIndent()
    val after = """
            A Discovery

            ${c}where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
            """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun `test line deletion multiline motion up`() {
    val keys = listOf("Vk", "D")
    val before = """
            A Discovery

            I found it in a legendary land
            all ${c}rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
            """.trimIndent()
    val after = """
            A Discovery

            ${c}where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
            """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun `test line delete visual lines end action`() {
    typeTextInFile(parseKeys("V", "2j", "D"),
      """
                    a${c}bcde
                    abcde
                    abcde
                    abcde
                    abcd${c}e
                    abcde
                    abcde

                    """.trimIndent())
    myFixture.checkResult("${c}abcd${c}e\n")
  }

  fun `test block simple deletion`() {
    val keys = listOf("<C-V>", "D")
    val before = """
            A Discovery

            I${c} found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    val after = """
            A Discovery

            I
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun `test block deletion empty line`() {
    val keys = listOf("<C-V>", "D")
    val before = """
            A Discovery
            ${c}
            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    val after = """
            A Discovery
            ${c}
            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun `test block deletion last line`() {
    val keys = listOf("<C-V>", "D")
    val before = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the${c} torrent of a mountain pass.

        """.trimIndent()
    val after = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the

        """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun `test block deletion last line without empty line`() {
    val keys = listOf("<C-V>", "D")
    val before = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the${c} torrent of a mountain pass.""".trimIndent()
    val after = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the""".trimIndent()
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun `test block deletion multiline`() {
    val keys = listOf("<C-V>j", "D")
    val before = """
            A Discovery

            I${c} found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
            """.trimIndent()
    val after = """
            A Discovery

            I
            a
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
            """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun `test block deletion multiline motion up`() {
    val keys = listOf("<C-V>k", "D")
    val before = """
            A Discovery

            I found it in a legendary land
            all${c} rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
            """.trimIndent()
    val after = """
            A Discovery

            I f
            all
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
            """.trimIndent()
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun `test delete visual block line end action`() {
    typeTextInFile(parseKeys("<C-V>", "2j", "2l", "D"),
      """
                    abcde
                    a${c}bcde
                    abcde
                    abcde
                    abcde

                    """.trimIndent())
    myFixture.checkResult(("""
    abcde
    ${c}a
    a
    a
    abcde

    """.trimIndent()))
  }
}
