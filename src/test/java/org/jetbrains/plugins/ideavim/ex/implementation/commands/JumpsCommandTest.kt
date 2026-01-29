/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.api.injector
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class JumpsCommandTest : VimTestCase() {
  @Test
  fun `test shows empty list`() {
    configureByText("")
    enterCommand("jumps")
    assertOutput(" jump line  col file/text\n>")
  }

  @Test
  fun `test show jump list`() {
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

    enterSearch("sodden")
    enterSearch("shape")
    enterSearch("rocks", false)
    enterSearch("underside")

    enterCommand("jumps")
    assertOutput(
      """ jump line  col file/text
                     |   4     1    8 I found it in a legendary land
                     |   3     3   29 where it was settled on some sodden sand
                     |   2     7   12 to science: shape and shade -- the special tinge,
                     |   1     2    4 all rocks and lavender and tufted grass,
                     |>
      """.trimMargin(),
    )
  }

  @Test
  fun `test highlights current jump spot`() {
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

    enterSearch("sodden")
    enterSearch("shape")
    enterSearch("rocks", false)
    enterSearch("underside")

    typeText(injector.parser.parseKeys("<C-O>" + "<C-O>"))

    enterCommand("jumps")
    assertOutput(
      """ jump line  col file/text
                     |   2     1    8 I found it in a legendary land
                     |   1     3   29 where it was settled on some sodden sand
                     |>  0     7   12 to science: shape and shade -- the special tinge,
                     |   1     2    4 all rocks and lavender and tufted grass,
                     |   2     9   10 the dingy underside, the checquered fringe.
      """.trimMargin(),
    )
  }

  @Test
  fun `test list trims and truncates`() {
    val indent = " ".repeat(100)
    val text = "Really long line ".repeat(1000)
    configureByText(indent + text)

    enterSearch("long")

    enterCommand("jumps")
    assertOutput(
      """ jump line  col file/text
                     |   1     1    0 ${text.substring(0, 200)}
                     |>
      """.trimMargin(),
    )
  }

  @Test
  fun `test correctly encodes non-printable characters`() {
    configureByText("\u0009Hello\u0006World\u007f")

    typeText(injector.parser.parseKeys("G"))

    enterCommand("jumps")
    assertOutput(
      """
        | jump line  col file/text
        |   1     1    0 Hello^FWorld^?
        |>
      """.trimMargin(),
    )
  }
}
