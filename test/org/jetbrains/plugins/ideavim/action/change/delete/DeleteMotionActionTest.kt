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

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.helper.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase

class DeleteMotionActionTest : VimTestCase() {

  @VimBehaviorDiffers(originalVimAfter = """
        def xxx():
          ${c}expression one
  """)
  fun `test delete last line`() {
    typeTextInFile(parseKeys("dd"),
      """
        def xxx():
          expression one
          expression${c} two
          """.trimIndent())
    myFixture.checkResult("""
        def xxx():
        ${c}  expression one
          """.trimIndent())
  }

  @VimBehaviorDiffers(originalVimAfter = "  expression two\n")
  fun `test delete last line stored with new line`() {
    typeTextInFile(parseKeys("dd"),
      """
        def xxx():
          expression one
          expression${c} two
          """.trimIndent())
    val savedText = VimPlugin.getRegister().lastRegister?.text ?: ""
    assertEquals("  expression two\n", savedText)
  }

  fun `test delete line action multicaret`() {
    typeTextInFile(parseKeys("d3d"),
      """
        abc${c}de
        abcde
        abcde
        abcde
        ab${c}cde
        abcde
        abcde
        
        """.trimIndent())
    myFixture.checkResult("${c}abcde\n${c}")
  }

  fun `test delete motion action multicaret`() {
    typeTextInFile(parseKeys("dt)"),
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
    myFixture.checkResult(
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
    typeTextInFile(parseKeys("dd"), file)
    myFixture.checkResult(newFile)
  }

  fun `test delete on last line`() {
    doTest("dd",
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
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE
    )
  }

  fun `test empty line`() {
    doTest("dd",
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
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE
    )
  }
}
