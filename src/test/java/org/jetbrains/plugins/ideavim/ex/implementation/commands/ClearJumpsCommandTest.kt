/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class ClearJumpsCommandTest : VimTestCase() {
  @Test
  fun `test clear jumps`() {
    configureByText(
      """
        |I found ${c}it in a legendary land
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
      """
        | jump line  col file/text
        |   4     1    8 I found it in a legendary land
        |   3     3   29 where it was settled on some sodden sand
        |   2     7   12 to science: shape and shade -- the special tinge,
        |   1     2    4 all rocks and lavender and tufted grass,
        |>
      """.trimMargin(),
    )

    enterCommand("clearjumps")

    enterCommand("jumps")
    assertOutput(
      """
        | jump line  col file/text
        |>
      """.trimMargin()
    )
  }

  @Test
  fun `test clear jumps abbreviation`() {
    configureByText(
      """
        |I found ${c}it in a legendary land
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
      """
        | jump line  col file/text
        |   4     1    8 I found it in a legendary land
        |   3     3   29 where it was settled on some sodden sand
        |   2     7   12 to science: shape and shade -- the special tinge,
        |   1     2    4 all rocks and lavender and tufted grass,
        |>
      """.trimMargin(),
    )

    enterCommand("clearju")

    enterCommand("jumps")
    assertOutput(
      """
      | jump line  col file/text
      |>
    """.trimMargin()
    )
  }
}
