/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

/**
 * Tests for `:tags`, see "h :tags"
 */
class TagsCommandTest : VimTestCase() {
  private val text = """
        I found ${c}it in a legendary land
        all rocks and lavender and tufted grass,
        where it was settled on some sodden sand
        hard by the torrent of a mountain pass.
  """.trimIndent()

  @Test
  fun `test shows empty stack`() {
    configureByText(text)
    enterCommand("tags")
    assertExOutput(
      """  # TO tag         FROM line  in file/text
        |>
      """.trimMargin(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test shows the tag jumped to and the line jumped from`() {
    configureByText(text)
    typeText("<C-]>")

    enterCommand("tags")
    assertExOutput(
      """  # TO tag         FROM line  in file/text
        |  1  1 it                  1  I found it in a legendary land
        |>
      """.trimMargin(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test shows several entries oldest first`() {
    configureByText(text)
    typeText("<C-]>")
    typeText("j0")
    typeText("<C-]>")

    enterCommand("tags")
    assertExOutput(
      """  # TO tag         FROM line  in file/text
        |  1  1 it                  1  I found it in a legendary land
        |  2  1 all                 2  all rocks and lavender and tufted grass,
        |>
      """.trimMargin(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test marks the current position in the stack`() {
    configureByText(text)
    typeText("<C-]>")
    typeText("j0")
    typeText("<C-]>")
    typeText("<C-T>")

    // At the top the marker is a line of its own, once we walk back it moves onto the entry we are at
    enterCommand("tags")
    assertExOutput(
      """  # TO tag         FROM line  in file/text
        |  1  1 it                  1  I found it in a legendary land
        |> 2  1 all                 2  all rocks and lavender and tufted grass,
      """.trimMargin(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test shows the stack again after walking all the way back`() {
    configureByText(text)
    typeText("<C-]>")
    typeText("j0")
    typeText("<C-]>")
    typeText("<C-T>")
    typeText("<C-T>")

    enterCommand("tags")
    assertExOutput(
      """  # TO tag         FROM line  in file/text
        |> 1  1 it                  1  I found it in a legendary land
        |  2  1 all                 2  all rocks and lavender and tufted grass,
      """.trimMargin(),
    )
  }
}
