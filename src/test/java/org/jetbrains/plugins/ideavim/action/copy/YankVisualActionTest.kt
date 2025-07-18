/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.action.copy

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.key.VimKeyStroke
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import javax.swing.KeyStroke
import kotlin.test.assertNotNull

class YankVisualActionTest : VimTestCase() {
  @Test
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
      "found",
      SelectionType.CHARACTER_WISE,
    )
  }

  @VimBehaviorDiffers("\n")
  @Test
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
      "",
      SelectionType.CHARACTER_WISE,
    )
  }

  @VimBehaviorDiffers("land\n")
  @Test
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
      "land",
      SelectionType.CHARACTER_WISE,
    )
  }

  @Test
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
    val editor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(editor)
    val lastRegister = injector.registerGroup.lastRegisterChar
    val registers = editor.carets().map { it.registerStorage.getRegister(editor, context, lastRegister)?.text }
    kotlin.test.assertEquals(listOf("found", "was"), registers)
  }

  // todo multicaret
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  @Disabled
  fun testYankVisualRange() {
    val before = """
            q${c}werty
            asdf${c}gh
            ${c}zxcvbn

    """.trimIndent()
    configureByText(before)
    typeText(injector.parser.parseKeys("vey"))

    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup
    val lastRegister = registerService.getRegister(vimEditor, context, registerService.lastRegisterChar)
    assertNotNull<Any>(lastRegister)
    val text = lastRegister.text
    assertNotNull<Any>(text)

    typeText(injector.parser.parseKeys("G" + "$" + "p"))
    val after = """
      qwerty
      asdfgh
      zxcvbn
      wert${c}yg${c}hzxcvb${c}n
    """.trimIndent()
    assertState(after)
  }

  @Test
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
      "I found it in a legendary land\n",
      SelectionType.LINE_WISE,
    )
  }

  @Test
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
      "hard by the torrent of a mountain pass.\n",
      SelectionType.LINE_WISE,
    )
  }

  @Test
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
    val editor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(editor)
    val lastRegister = injector.registerGroup.lastRegisterChar
    val registers = editor.carets().map { it.registerStorage.getRegister(editor, context, lastRegister)?.text }
    kotlin.test.assertEquals(
      listOf("all rocks and lavender and tufted grass,\n", "hard by the torrent of a mountain pass.\n"),
      registers,
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
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

  @Test
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
      "fo\nl ",
      SelectionType.BLOCK_WISE,
    )
  }

  @Test
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
      SelectionType.BLOCK_WISE,
    )
  }

  @Test
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
      SelectionType.BLOCK_WISE,
    )
  }

  @Test
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
      SelectionType.CHARACTER_WISE,
    )
  }

  @Test
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
      SelectionType.LINE_WISE,
    )
  }

  private fun doTest(keys: List<VimKeyStroke>, before: String, expectedText: String, expectedType: SelectionType) {
    configureByText(before)
    typeText(keys)

    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup
    val lastRegister = registerService.getRegister(vimEditor, context, registerService.lastRegisterChar)!!
    val text = lastRegister.text
    val type = lastRegister.type
    kotlin.test.assertEquals(expectedText, text)
    kotlin.test.assertEquals(expectedType, type)
  }
}
