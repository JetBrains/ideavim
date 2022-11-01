/*
 * Copyright 2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.action.change.delete

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.helper.VimBehaviorDiffers
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class DeleteMotionActionTest : VimTestCase() {

  fun `test delete last line`() {
    typeTextInFile(
      injector.parser.parseKeys("dd"),
      """
        def xxx():
          expression one
          expression${c} two
      """.trimIndent()
    )
    assertState(
      """
        def xxx():
          ${c}expression one
      """.trimIndent()
    )
  }

  fun `test on line in middle`() {
    typeTextInFile(
      injector.parser.parseKeys("dd"),
      """
        def xxx():
          expression${c} one
          expression two
      """.trimIndent()
    )
    assertState(
      """
        def xxx():
          ${c}expression two
      """.trimIndent()
    )
  }

  fun `test delete single line`() {
    typeTextInFile(
      injector.parser.parseKeys("dd"),
      """
        def x${c}xx():
      """.trimIndent()
    )
    assertState(c)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  fun `test delete last line with nostartofline`() {
    VimPlugin.getOptionService().unsetOption(OptionScope.GLOBAL, OptionConstants.startoflineName)
    typeTextInFile(
      injector.parser.parseKeys("dd"),
      """
        |def xxx():
        |  expression one
        |  expression${c} two
      """.trimMargin()
    )
    assertState(
      """
        |def xxx():
        |  expression${c} one
      """.trimMargin()
    )
  }

  @VimBehaviorDiffers(originalVimAfter = "  expression two\n")
  fun `test delete last line stored with new line`() {
    typeTextInFile(
      injector.parser.parseKeys("dd"),
      """
        def xxx():
          expression one
          expression${c} two
      """.trimIndent()
    )
    val savedText = VimPlugin.getRegister().lastRegister?.text ?: ""
    assertEquals("  expression two\n", savedText)
  }

  fun `test delete line action multicaret`() {
    typeTextInFile(
      injector.parser.parseKeys("d3d"),
      """
        abc${c}de
        abcde
        abcde
        abcde
        ab${c}cde
        abcde
        abcde
        
      """.trimIndent()
    )
    assertState("${c}abcde\n${c}")
  }

  fun `test delete motion action multicaret`() {
    typeTextInFile(
      injector.parser.parseKeys("dt)"),
      """|public class Foo {
         |  int foo(int a, int b) {
         |    boolean bar = (a < 0 && (b < 0 || a > 0)${c} || b != 0);
         |    if (bar${c} || b != 0) {
         |      return a;
         |    }
         |    else {
         |      return b;
         |    }
         |  }
         |}
        """.trimMargin()
    )
    assertState(
      """|public class Foo {
         |  int foo(int a, int b) {
         |    boolean bar = (a < 0 && (b < 0 || a > 0)${c});
         |    if (bar${c}) {
         |      return a;
         |    }
         |    else {
         |      return b;
         |    }
         |  }
         |}
         """.trimMargin()
    )
  }

  fun `test delete empty line`() {
    val file = """
            A Discovery
            ${c}
            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val newFile = """
            A Discovery
            ${c}I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    typeTextInFile(injector.parser.parseKeys("dd"), file)
    assertState(newFile)
  }

  fun `test delete on last line`() {
    doTest(
      "dd",
      """
            A Discovery
            
            I found it in a legendary land
            all rocks and lavender and tufted grass,
            ${c}
      """.trimIndent(),
      """
            A Discovery
            
            I found it in a legendary land
            ${c}all rocks and lavender and tufted grass,
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }

  fun `test empty line`() {
    doTest(
      "dd",
      """
            A Discovery
            
            ${c}
            
            
            I found it in a legendary land
            all rocks and lavender and tufted grass,
      """.trimIndent(),
      """
            A Discovery
            
            ${c}
            
            I found it in a legendary land
            all rocks and lavender and tufted grass,
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE
    )
  }
}
