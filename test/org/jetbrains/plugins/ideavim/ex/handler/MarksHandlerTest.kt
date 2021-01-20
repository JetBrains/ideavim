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

package org.jetbrains.plugins.ideavim.ex.handler

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.VimTestCase

class MarksHandlerTest : VimTestCase() {
  fun `test list empty marks`() {
    configureByText("")
    enterCommand("marks")
    assertExOutput("mark line  col file/text\n")
  }

  fun `test list simple mark`() {
    configureByText("""I found it in a legendary land
                      |all rocks and lavender and tufted grass,
                      |where it$c was settled on some sodden sand
                      |hard by the torrent of a mountain pass.
                    """.trimMargin())
    typeText(parseKeys("ma"))

    enterCommand("marks")
    assertExOutput("""mark line  col file/text
                     | a      3    8 where it was settled on some sodden sand
    """.trimMargin())
  }

  fun `test line number is 1-based and column is 0-based`() {
    configureByText("""${c}I found it in a legendary land
                      |all rocks and lavender and tufted grass,
                      |where it was settled on some sodden sand
                      |hard by the torrent of a mountain pass.
                    """.trimMargin())
    typeText(parseKeys("ma"))

    enterCommand("marks")
    assertExOutput("""mark line  col file/text
                     | a      1    0 I found it in a legendary land
    """.trimMargin())

  }

  fun `test list multiple marks`() {
    configureByText("""I found ${c}it in a legendary land
                         |all rocks and lavender and tufted grass,
                         |where it was settled on some sodden sand
                         |hard by the torrent of a mountain pass.
                       """.trimMargin())
    typeText(parseKeys("ma", "jl"))
    typeText(parseKeys("mb", "jl"))
    typeText(parseKeys("mc", "jl"))
    typeText(parseKeys("md"))

    enterCommand("marks")
    assertExOutput("""mark line  col file/text
                     | a      1    8 I found it in a legendary land
                     | b      2    9 all rocks and lavender and tufted grass,
                     | c      3   10 where it was settled on some sodden sand
                     | d      4   11 hard by the torrent of a mountain pass.
    """.trimMargin())
  }

  fun `test lists global marks`() {
    configureByText("""I found ${c}it in a legendary land
                         |all rocks and lavender and tufted grass,
                         |where it was settled on some sodden sand
                         |hard by the torrent of a mountain pass.
                       """.trimMargin())
    typeText(parseKeys("mA", "jll"))
    typeText(parseKeys("mB"))

    enterCommand("marks")
    assertExOutput("""mark line  col file/text
                     | A      1    8 I found it in a legendary land
                     | B      2   10 all rocks and lavender and tufted grass,
    """.trimMargin())
  }

  fun `test argument filters output`() {
    configureByText("""I found ${c}it in a legendary land
                         |all rocks and lavender and tufted grass,
                         |where it was settled on some sodden sand
                         |hard by the torrent of a mountain pass.
                       """.trimMargin())
    typeText(parseKeys("ma", "jl"))
    typeText(parseKeys("mb", "jl"))
    typeText(parseKeys("mc", "jl"))
    typeText(parseKeys("mD"))

    enterCommand("marks bdD")
    assertExOutput("""mark line  col file/text
                     | b      2    9 all rocks and lavender and tufted grass,
                     | D      4   11 hard by the torrent of a mountain pass.
    """.trimMargin())
  }

  fun `test list nothing if no marks match`() {
    configureByText("""I found ${c}it in a legendary land
                         |all rocks and lavender and tufted grass,
                         |where it was settled on some sodden sand
                         |hard by the torrent of a mountain pass.
                       """.trimMargin())
    typeText(parseKeys("ma", "jl"))
    enterCommand("marks b")
    assertExOutput("mark line  col file/text\n")
  }

  fun `test correctly handles invalid mark location`() {
    configureByText("""I found ${c}it in a legendary land
                      |all rocks and lavender and tufted grass,
                      |where it was settled on some sodden sand
                      |hard by the torrent of a mountain pass.
                      """.trimMargin())
    VimPlugin.getMark().setMark(myFixture.editor, 'a', 100000)
    enterCommand("marks")
    assertExOutput("""mark line  col file/text
                     | a      4   39 hard by the torrent of a mountain pass.
    """.trimMargin())
  }

  fun `test correctly encodes non printable characters`() {
    configureByText("$c\u0009Hello world\n\u0006\n\u007f")
    typeText(parseKeys("ma", "j", "mb", "j", "mc"))
    enterCommand("marks abc")
    assertExOutput("""mark line  col file/text
                     | a      1    0 Hello world
                     | b      2    0 ^F
                     | c      3    0 ^?
    """.trimMargin())
  }

  fun `test list trims and truncates`() {
    val indent = " ".repeat(100)
    val text = "Really long line ".repeat(1000)
    configureByText(indent + c + text)
    typeText(parseKeys("ma"))
    enterCommand("marks a")
    assertExOutput("""mark line  col file/text
                     | a      1  100 ${text.substring(0, 200)}
    """.trimMargin())
  }

  fun `test list all marks in correct order`() {
    configureByText("""I found ${c}it in a legendary land
                      |all rocks and lavender and tufted grass,
                      |where it was settled on some sodden sand
                      |hard by the torrent of a mountain pass.
                      |
                      |The features it combines mark it as new
                      |to science: shape and shade -- the special tinge,
                      |akin to moonlight, tempering its blue,
                      |the dingy underside, the checquered fringe.
                      """.trimMargin())
    typeText(parseKeys("ma", "w", "mb", "2w", "j")) // a + b
    typeText(parseKeys("v2b", "<Esc>", "j")) // < and > - last visual selection marks
    typeText(parseKeys("2b", "mB", "j", "mA", "<CR><CR>w")) // A + B
    typeText(parseKeys("i", "inserted text ", "<Esc>", "<CR><CR>"))  // ^ - position of end of last insert. Also '.' for start of change
    typeText(parseKeys("w", "c4w", "replaced content", "<Esc>"))  // [ and ] - recently changed/yanked
    typeText(parseKeys("gg")) // ' - position before last jump

    // Vim does not list the (, ), { or } marks. See :help :marks
    // Can't easily test 0-9 or " (locations of previously closed files from vim-info)
    enterCommand("marks")
    assertExOutput("""mark line  col file/text
                     | '      8   20 akin replaced content its blue,
                     | a      1    8 I found it in a legendary land
                     | b      1   11 I found it in a legendary land
                     | A      4    6 hard by the torrent of a mountain pass.
                     | B      3    6 where it was settled on some sodden sand
                     | [      8    5 akin replaced content its blue,
                     | ]      8   21 akin replaced content its blue,
                     | ^      8   21 akin replaced content its blue,
                     | .      8   20 akin replaced content its blue,
                     | <      2   16 all rocks and lavender and tufted grass,
                     | >      2   10 all rocks and lavender and tufted grass,
                     """.trimMargin())
  }
}
