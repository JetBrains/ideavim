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
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class ChangeVisualActionTest : VimTestCase() {
  @Test
  fun `test multiple line change`() {
    val keys = "VjcHello<esc>"
    val before = """
            ${c}Lorem Ipsum

            Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    val after = """
            Hello
            Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    doTest(keys, before, after, Mode.NORMAL())
  }

  @Test
  fun `test multiple line change in text middle`() {
    val keys = "Vjc"
    val before = """
            Lorem Ipsum

            ${c}Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    val after = """
            Lorem Ipsum

            ${c}
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    doTest(keys, before, after, Mode.INSERT)
  }

  @VimBehaviorDiffers(
    originalVimAfter = """
            Lorem Ipsum

            Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            
            ${c}
  """,
  )
  @Test
  fun `test multiple line change till the end`() {
    val keys = "Vjc"
    val before = """
            Lorem Ipsum

            Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            
            ${c}Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    val after = """
            Lorem Ipsum

            Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            ${c}
            
    """.trimIndent()
    doTest(keys, before, after, Mode.INSERT)
  }

  @Test
  fun `test multiple line change till the end with two new lines`() {
    val keys = "Vjc"
    val before = """
            Lorem Ipsum

            Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            
            ${c}Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
            
            
    """.trimIndent()
    val after = """
            Lorem Ipsum

            Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
           
            ${c}
            
            
    """.trimIndent()
    doTest(keys, before, after, Mode.INSERT)
  }

  @VimBehaviorDiffers(description = "Wrong caret position")
  @Test
  fun `test change with dollar motion`() {
    val keys = listOf("<C-V>3j$", "c", "Hello<Esc>")
    val before = """
            Lorem Ipsum

            I |${c}found it in a legendary land
            al|l rocks and lavender and tufted grass,[ additional symbols]
            wh|ere it was settled on some sodden sand
            ha|rd by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
            Lorem Ipsum

            I |Hello
            al|Hello
            wh|Hello
            ha|Hello
    """.trimIndent()
    doTest(keys, before, after, Mode.NORMAL())
  }

  @Test
  fun `test replace first line`() {
    val keys = "VcHello<esc>"
    val before = "${c}Lorem Ipsum"
    val after = "Hello"
    doTest(keys, before, after, Mode.NORMAL())
  }

  @Test
  fun `test change visual action`() {
    typeTextInFile(
      injector.parser.parseKeys("v2lc" + "aaa" + "<ESC>"),
      "abcd${c}ffffff${c}abcde${c}aaaa\n",
    )
    assertMode(Mode.NORMAL())
    assertState("abcdaa${c}afffaa${c}adeaa${c}aa\n")
  }

  // VIM-1379 |CTRL-V| |j| |v_b_c|
  @VimBehaviorDiffers(description = "Different caret position")
  @Test
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
      Mode.NORMAL(),
    )
  }

  // VIM-1379 |CTRL-V| |j| |v_b_c|
  @VimBehaviorDiffers(description = "Different caret position")
  @Test
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
      Mode.NORMAL(),
    )
  }
}
