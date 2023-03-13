/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.action.change.change

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.helper.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase

class ChangeVisualActionTest : VimTestCase() {
  fun `test multiple line change`() {
    val keys = "VjcHello<esc>"
    val before = """
            ${c}A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
            Hello
            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  fun `test multiple line change in text middle`() {
    val keys = "Vjc"
    val before = """
            A Discovery

            ${c}I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
            A Discovery

            ${c}
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    doTest(keys, before, after, VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE)
  }

  @VimBehaviorDiffers(
    originalVimAfter = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            
            ${c}
  """,
  )
  fun `test multiple line change till the end`() {
    val keys = "Vjc"
    val before = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            
            ${c}where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            ${c}
            
    """.trimIndent()
    doTest(keys, before, after, VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE)
  }

  fun `test multiple line change till the end with two new lines`() {
    val keys = "Vjc"
    val before = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            
            ${c}where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
            
            
    """.trimIndent()
    val after = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
           
            ${c}
            
            
    """.trimIndent()
    doTest(keys, before, after, VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE)
  }

  @VimBehaviorDiffers(description = "Wrong caret position")
  fun `test change with dollar motion`() {
    val keys = listOf("<C-V>3j$", "c", "Hello<Esc>")
    val before = """
            A Discovery

            I |${c}found it in a legendary land
            al|l rocks and lavender and tufted grass,[ additional symbols]
            wh|ere it was settled on some sodden sand
            ha|rd by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
            A Discovery

            I |Hello
            al|Hello
            wh|Hello
            ha|Hello
    """.trimIndent()
    doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  fun `test replace first line`() {
    val keys = "VcHello<esc>"
    val before = "${c}A Discovery"
    val after = "Hello"
    doTest(keys, before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  fun `test change visual action`() {
    typeTextInFile(
      injector.parser.parseKeys("v2lc" + "aaa" + "<ESC>"),
      "abcd${c}ffffff${c}abcde${c}aaaa\n",
    )
    assertMode(VimStateMachine.Mode.COMMAND)
    assertState("abcdaa${c}afffaa${c}adeaa${c}aa\n")
  }

  // VIM-1379 |CTRL-V| |j| |v_b_c|
  @VimBehaviorDiffers(description = "Different caret position")
  fun `test change visual block with empty line in the middle`() {
    doTest(
      listOf("ll", "<C-V>", "ljjc", "_quux_", "<Esc>"),
      """
        foo foo
        
        bar bar
        
      """.trimIndent(),
      """
        fo_quux_foo
        
        ba_quux_bar
        
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
  }

  // VIM-1379 |CTRL-V| |j| |v_b_c|
  @VimBehaviorDiffers(description = "Different caret position")
  fun `test change visual block with shorter line in the middle`() {
    doTest(
      listOf("ll", "<C-V>", "ljjc", "_quux_", "<Esc>"),
      """
        foo foo
        x
        bar bar
        
      """.trimIndent(),
      """
        fo_quux_foo
        x
        ba_quux_bar
        
      """.trimIndent(),
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
  }
}
