/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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

package org.jetbrains.plugins.ideavim.action.copy

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.helper.VimBehaviorDiffers
import com.maddyhome.idea.vim.newapi.vim
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import javax.swing.KeyStroke

class YankVisualActionTest : VimTestCase() {
  fun `test simple yank`() {
    doTest(
      injector.parser.parseKeys("viw" + "y"),
      """
                            A Discovery

                            I ${c}found it in a legendary land
                            all rocks and lavender and tufted grass,
                            where it was settled on some sodden sand
                            hard by the torrent of a mountain pass.
      """.trimIndent(),
      "found", SelectionType.CHARACTER_WISE
    )
  }

  @VimBehaviorDiffers("\n")
  fun `test yank empty line`() {
    doTest(
      injector.parser.parseKeys("v" + "y"),
      """
                            A Discovery
                            ${c}
                            I found it in a legendary land
                            all rocks and lavender and tufted grass,
                            where it was settled on some sodden sand
                            hard by the torrent of a mountain pass.
      """.trimIndent(),
      "", SelectionType.CHARACTER_WISE
    )
  }

  @VimBehaviorDiffers("land\n")
  fun `test yank to the end`() {
    doTest(
      injector.parser.parseKeys("viwl" + "y"),
      """
                            A Discovery

                            I found it in a legendary ${c}land
                            all rocks and lavender and tufted grass,
                            where it was settled on some sodden sand
                            hard by the torrent of a mountain pass.
      """.trimIndent(),
      "land", SelectionType.CHARACTER_WISE
    )
  }

  fun `test yank multicaret`() {
    val text = """
                            A Discovery

                            I ${c}found it in a legendary land
                            all rocks and lavender and tufted grass,
                            where it ${c}was settled on some sodden sand
                            hard by the torrent of a mountain pass.
    """.trimIndent()
    configureByText(text)
    typeText(injector.parser.parseKeys("viw" + "y"))
    val editor = myFixture.editor.vim
    val lastRegister = injector.registerGroup.lastRegisterChar
    val registers = editor.carets().map { it.registerStorage.getRegister(it, lastRegister)?.rawText }
    assertEquals(listOf("found", "was"), registers)
  }

  // todo multicaret
//  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
//  fun testYankVisualRange() {
//    val before = """
//            q${c}werty
//            asdf${c}gh
//            ${c}zxcvbn
//
//    """.trimIndent()
//    configureByText(before)
//    typeText(injector.parser.parseKeys("vey"))
//
//    val lastRegister = VimPlugin.getRegister().lastRegister
//    TestCase.assertNotNull(lastRegister)
//    val text = lastRegister!!.text
//    TestCase.assertNotNull(text)
//
//    typeText(injector.parser.parseKeys("G" + "$" + "p"))
//    val after = """
//      qwerty
//      asdfgh
//      zxcvbn
//      wert${c}yg${c}hzxcvb${c}n
//    """.trimIndent()
//    assertState(after)
//  }

  fun `test yank line`() {
    doTest(
      injector.parser.parseKeys("V" + "y"),
      """
                            A Discovery

                            I ${c}found it in a legendary land
                            all rocks and lavender and tufted grass,
                            where it was settled on some sodden sand
                            hard by the torrent of a mountain pass.
      """.trimIndent(),
      "I found it in a legendary land\n", SelectionType.LINE_WISE
    )
  }

  fun `test yank last line`() {
    doTest(
      injector.parser.parseKeys("V" + "y"),
      """
                            A Discovery

                            I found it in a legendary land
                            all rocks and lavender and tufted grass,
                            where it was settled on some sodden sand
                            hard by ${c}the torrent of a mountain pass.
      """.trimIndent(),
      "hard by the torrent of a mountain pass.\n", SelectionType.LINE_WISE
    )
  }

  fun `test yank multicaret line`() {
    val text = """
                            A Discovery

                            I found it in a legendary land
                            all ${c}rocks and lavender and tufted grass,
                            where it was settled on some sodden sand
                            hard by ${c}the torrent of a mountain pass.
    """.trimIndent()
    configureByText(text)
    typeText(injector.parser.parseKeys("V" + "y"))
    val editor = myFixture.editor.vim
    val lastRegister = injector.registerGroup.lastRegisterChar
    val registers = editor.carets().map { it.registerStorage.getRegister(it, lastRegister)?.rawText }
    assertEquals(
      listOf("all rocks and lavender and tufted grass,\n", "hard by the torrent of a mountain pass.\n"),
      registers
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  fun testYankVisualLines() {
    val before = """
            q${c}we
            asd
            z${c}xc
            rt${c}y
            fgh
            vbn
            
    """.trimIndent()
    configureByText(before)
    typeText(injector.parser.parseKeys("Vy"))

    typeText(injector.parser.parseKeys("p"))
    val after = """
            qwe
            ${c}qwe
            asd
            zxc
            ${c}zxc
            rty
            ${c}rty
            fgh
            vbn
            
    """.trimIndent()
    assertState(after)
  }

  fun `test block yank`() {
    doTest(
      injector.parser.parseKeys("<C-V>lj" + "y"),
      """
                            A Discovery

                            I ${c}found it in a legendary land
                            all rocks and lavender and tufted grass,
                            where it was settled on some sodden sand
                            hard by the torrent of a mountain pass.
      """.trimIndent(),
      "fo\nl ", SelectionType.BLOCK_WISE
    )
  }

  fun `test block yank with dollar motion`() {
    doTest(
      injector.parser.parseKeys("<C-V>3j$" + "y"),
      """
                            A Discovery

                            I ${c}found it in a legendary land
                            all rocks and lavender and tufted grass,[ additional symbols]
                            where it was settled on some sodden sand
                            hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                    found it in a legendary land
                    l rocks and lavender and tufted grass,[ additional symbols]
                    ere it was settled on some sodden sand
                    rd by the torrent of a mountain pass.
      """.trimIndent(),
      SelectionType.BLOCK_WISE
    )
  }

  fun `test block yank with dollar motion backward`() {
    doTest(
      injector.parser.parseKeys("<C-V>k$" + "y"),
      """
                            A Discovery

                            I found it in a legendary land
                            al${c}l rocks and lavender and tufted grass,[ additional symbols]
                            where it was settled on some sodden sand
                            hard by the torrent of a mountain pass.
      """.trimIndent(),
      """
                    found it in a legendary land
                    l rocks and lavender and tufted grass,[ additional symbols]
      """.trimIndent(),
      SelectionType.BLOCK_WISE
    )
  }

  fun `test yank to numbered register in visual`() {
    doTest(
      injector.parser.parseKeys("ve" + "\"2y"),
      """
                            A Discovery

                            I found it in a legendary land
                            all ${c}rocks and lavender and tufted grass,[ additional symbols]
                            where it was settled on some sodden sand
                            hard by the torrent of a mountain pass.
      """.trimIndent(),
      "rocks",
      SelectionType.CHARACTER_WISE
    )
  }

  fun `test yank to numbered register`() {
    doTest(
      injector.parser.parseKeys("\"2yy"),
      """
                            A Discovery

                            I found it in a legendary land
                            all ${c}rocks and lavender and tufted grass,[ additional symbols]
                            where it was settled on some sodden sand
                            hard by the torrent of a mountain pass.
      """.trimIndent(),
      "all rocks and lavender and tufted grass,[ additional symbols]\n",
      SelectionType.LINE_WISE
    )
  }

  private fun doTest(keys: List<KeyStroke>, before: String, expectedText: String, expectedType: SelectionType) {
    configureByText(before)
    typeText(keys)

    val lastRegister = VimPlugin.getRegister().lastRegister!!
    val text = lastRegister.text
    val type = lastRegister.type
    assertEquals(expectedText, text)
    assertEquals(expectedType, type)
  }
}
