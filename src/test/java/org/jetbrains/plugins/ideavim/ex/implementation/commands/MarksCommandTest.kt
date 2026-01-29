/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.intellij.idea.TestFor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.vim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class MarksCommandTest : VimTestCase() {
  @Test
  @TestFor(issues = ["VIM-3176"])
  fun `test gv after pasting to the same line`() {
    configureByText(
      """${c}I found it in a legendary land
                      |all rocks and lavender and tufted grass,
                      |where it was settled on some sodden sand
                      |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    typeText(injector.parser.parseKeys("V3j" + "y" + "P" + "gv"))
    assertState(
      """I found it in a legendary land
                      |all rocks and lavender and tufted grass,
                      |where it was settled on some sodden sand
                      |hard by the torrent of a mountain pass.
                      |${s}I found it in a legendary land
                      |all rocks and lavender and tufted grass,
                      |where it was settled on some sodden sand
                      |${c}hard by the torrent of a mountain pass.${se}
      """.trimMargin(),
    )
  }

  @Test
  @TestFor(issues = ["VIM-3176"])
  fun `test gv after pasting to the same line reversed selection`() {
    configureByText(
      """I found it in a legendary land
                      |all rocks and lavender and tufted grass,
                      |where it was settled on some sodden sand
                      |${c}hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    typeText(injector.parser.parseKeys("V3k" + "y" + "P" + "gv"))
    assertState(
      """I found it in a legendary land
                      |all rocks and lavender and tufted grass,
                      |where it was settled on some sodden sand
                      |hard by the torrent of a mountain pass.
                      |${s}${c}I found it in a legendary land
                      |all rocks and lavender and tufted grass,
                      |where it was settled on some sodden sand
                      |hard by the torrent of a mountain pass.${se}
      """.trimMargin(),
    )
  }

  @Test
  @TestFor(issues = ["VIM-3176"])
  fun `test gv after pasting inside selection expanded selection`() {
    configureByText(
      """
     ${c}line1
     line2
      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("Vj" + "y" + "j" + "P" + "gv"))
    assertState(
      """
    ${s}line1
    line1
    line2
    line2${se}
      """.trimIndent(),
    )
  }

  @Test
  @TestFor(issues = ["VIM-3176"])
  fun `test gv after pasting below selection not changing selection`() {
    configureByText(
      """
     ${c}line1
     line2
     not selected
      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("Vj" + "y" + "j" + "p" + "gv"))
    assertState(
      """
    ${s}line1
    line2
    ${se}line1
    line2
    not selected
      """.trimIndent(),
    )
  }

  @Test
  @TestFor(issues = ["VIM-2223"])
  fun `test gv after replacing a line`() {
    configureByText(
      """I found it in a legendary land
                      |all rocks$c and lavender and tufted grass,
                      |where it was settled on some sodden sand
                      |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    typeText(injector.parser.parseKeys("VyjVpgv"))
    assertState(
      """I found it in a legendary land
                      |all rocks and lavender and tufted grass,
                      |${s}all rocks and lavender and tufted grass,
                      |${se}hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
  }

  @Test
  @TestFor(issues = ["VIM-1684"])
  fun `test reselecting different text length`() {
    configureByText(
      """
      # (response.get${c}_data(as_text=True))
      # (response.data.decode("utf-8"))
      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("vi)yjvi)pgv"))
    assertState(
      """
      # (response.get_data(as_text=True))
      # (response.get_data(as_text=True))
      """.trimIndent(),
    )
  }

  @Test
  @TestFor(issues = ["VIM-2491"])
  fun `test mapping with gv`() {
    configureByText("Oh, hi ${c}Andy Tom John")
    typeText(commandToKeys("xnoremap p pgvy"))
    typeText(injector.parser.parseKeys("yewvepwvep"))
    assertState("Oh, hi Andy Andy Andy")
  }

  @Test
  fun `test list empty marks`() {
    configureByText("")
    enterCommand("marks")
    assertOutput("mark line  col file/text")
  }

  @Test
  fun `test list simple mark`() {
    configureByText(
      """I found it in a legendary land
                      |all rocks and lavender and tufted grass,
                      |where it$c was settled on some sodden sand
                      |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    typeText(injector.parser.parseKeys("ma"))

    enterCommand("marks")
    assertOutput(
      """
        |mark line  col file/text
        | a      3    8 where it was settled on some sodden sand
      """.trimMargin(),
    )
  }

  @Test
  fun `test line number is 1-based and column is 0-based`() {
    configureByText(
      """${c}Lorem ipsum dolor sit amet,
                      |consectetur adipiscing elit
                      |Sed in orci mauris.
                      |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
    )
    typeText(injector.parser.parseKeys("ma"))

    enterCommand("marks")
    assertOutput(
      """
        |mark line  col file/text
        | a      1    0 Lorem ipsum dolor sit amet,
      """.trimMargin(),
    )
  }

  @Test
  fun `test list multiple marks`() {
    configureByText(
      """I found ${c}it in a legendary land
                         |all rocks and lavender and tufted grass,
                         |where it was settled on some sodden sand
                         |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    typeText(injector.parser.parseKeys("ma" + "jl"))
    typeText(injector.parser.parseKeys("mb" + "jl"))
    typeText(injector.parser.parseKeys("mc" + "jl"))
    typeText(injector.parser.parseKeys("md"))

    enterCommand("marks")
    assertOutput(
      """
        |mark line  col file/text
        | a      1    8 I found it in a legendary land
        | b      2    9 all rocks and lavender and tufted grass,
        | c      3   10 where it was settled on some sodden sand
        | d      4   11 hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
  }

  @Test
  fun `test lists global marks`() {
    configureByText(
      """I found ${c}it in a legendary land
                         |all rocks and lavender and tufted grass,
                         |where it was settled on some sodden sand
                         |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
    typeText(injector.parser.parseKeys("mA" + "jll"))
    typeText(injector.parser.parseKeys("mB"))

    enterCommand("marks")
    assertOutput(
      """
        |mark line  col file/text
        | A      1    8 I found it in a legendary land
        | B      2   10 all rocks and lavender and tufted grass,
      """.trimMargin(),
    )
  }

  @Test
  fun `test argument filters output`() {
    configureByText(
      """I found ${c}it in a legendary land
                         |consectetur adipiscing elit
                         |Sed in orci mauris.
                         |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
    )
    typeText(injector.parser.parseKeys("ma" + "jl"))
    typeText(injector.parser.parseKeys("mb" + "jl"))
    typeText(injector.parser.parseKeys("mc" + "jl"))
    typeText(injector.parser.parseKeys("mD"))

    enterCommand("marks bdD")
    assertOutput(
      """
        |mark line  col file/text
        | b      2    9 consectetur adipiscing elit
        | D      4   11 Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
    )
  }

  @Test
  fun `test list nothing if no marks match`() {
    configureByText(
      """I found ${c}it in a legendary land
                         |consectetur adipiscing elit
                         |Sed in orci mauris.
                         |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
    )
    typeText(injector.parser.parseKeys("ma" + "jl"))
    enterCommand("marks b")
    assertOutput("mark line  col file/text")
  }

  @Test
  fun `test correctly handles invalid mark location`() {
    configureByText(
      """I found ${c}it in a legendary land
                      |consectetur adipiscing elit
                      |Sed in orci mauris.
                      |Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
    )
    val vimEditor = fixture.editor.vim
    injector.markService.setMark(vimEditor.primaryCaret(), 'a', 100000)
    enterCommand("marks")
    assertOutput(
      """
        |mark line  col file/text
        | a      4   39 Cras id tellus in ex imperdiet egestas.
      """.trimMargin(),
    )
  }

  @Test
  fun `test correctly encodes non printable characters`() {
    configureByText("$c\u0009Hello world\n\u0006\n\u007f")
    typeText(injector.parser.parseKeys("ma" + "j" + "mb" + "j" + "mc"))
    enterCommand("marks abc")
    assertOutput(
      """
        |mark line  col file/text
        | a      1    0 Hello world
        | b      2    0 ^F
        | c      3    0 ^?
      """.trimMargin(),
    )
  }

  @Test
  fun `test list trims and truncates`() {
    val indent = " ".repeat(100)
    val text = "Really long line ".repeat(1000)
    configureByText(indent + c + text)
    typeText(injector.parser.parseKeys("ma"))
    enterCommand("marks a")
    assertOutput(
      """
        |mark line  col file/text
        | a      1  100 ${text.substring(0, 200)}
      """.trimMargin(),
    )
  }

  @Test
  fun `test list all marks in correct order`() {
    configureByText(
      """I found ${c}it in a legendary land
                      |all rocks and lavender and tufted grass,
                      |where it was settled on some sodden sand
                      |hard by the torrent of a mountain pass.
                      |
                      |The features it combines mark it as new
                      |to science: shape and shade -- the special tinge,
                      |akin to moonlight, tempering its blue,
                      |the dingy underside, the checquered fringe.
      """.trimMargin(),
    )
    typeText(injector.parser.parseKeys("ma" + "w" + "mb" + "2w" + "j")) // a + b
    typeText(injector.parser.parseKeys("v2b" + "<Esc>" + "j")) // < and > - last visual selection marks
    typeText(injector.parser.parseKeys("2b" + "mB" + "j" + "mA" + "<CR><CR>w")) // A + B
    typeText(
      injector.parser.parseKeys(
        "i" +
          "inserted text " +
          "<Esc>" +
          "<CR><CR>",
      ),
    ) // ^ - position of end of last insert. Also '.' for start of change
    typeText(injector.parser.parseKeys("w" + "c4w" + "replaced content" + "<Esc>")) // [ and ] - recently changed/yanked
    typeText(injector.parser.parseKeys("gg")) // ' - position before last jump

    // Vim does not list the (, ), { or } marks. See :help :marks
    // Can't easily test 0-9 or " (locations of previously closed files from vim-info)
    enterCommand("marks")
    assertOutput(
      """
        |mark line  col file/text
        | '      8   20 akin replaced content its blue,
        | a      1    8 I found it in a legendary land
        | b      1   11 I found it in a legendary land
        | A      4    6 hard by the torrent of a mountain pass.
        | B      3    6 where it was settled on some sodden sand
        | [      8    5 akin replaced content its blue,
        | ]      8   21 akin replaced content its blue,
        | ^      8   21 akin replaced content its blue,
        | .      8   20 akin replaced content its blue,
        | <      2   10 all rocks and lavender and tufted grass,
        | >      2   16 all rocks and lavender and tufted grass,
      """.trimMargin(),
    )
  }
}
