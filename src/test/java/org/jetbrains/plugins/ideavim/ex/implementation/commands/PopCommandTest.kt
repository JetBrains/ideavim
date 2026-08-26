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
 * Tests for `:pop`, see "h :pop"
 *
 * `:[count]pop` walks [count] entries back up the tag stack, exactly like `{count}<C-T>` - the count is relative, not
 * the number of the entry to land on. Verified against Nvim 0.12.2.
 */
class PopCommandTest : VimTestCase() {
  private val text = """
        I found ${c}it in a legendary land
        all rocks and lavender and tufted grass,
        where it was settled on some sodden sand
        hard by the torrent of a mountain pass.
  """.trimIndent()

  /**
   * Leaves three entries on the stack, jumped from (0, 8), (1, 0) and (2, 0), with the caret on the last line
   */
  private fun configureWithThreeTagJumps() {
    configureByText(text)
    typeText("<C-]>")
    typeText("j0")
    typeText("<C-]>")
    typeText("j0")
    typeText("<C-]>")
    typeText("G")
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test pop walks one entry back`() {
    configureWithThreeTagJumps()

    enterCommand("pop")

    assertPluginError(false)
    assertPosition(2, 0)
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test pop with count walks several entries back`() {
    configureWithThreeTagJumps()

    enterCommand("2pop")

    assertPosition(1, 0)
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test pop with count reaching the bottom of the stack`() {
    configureWithThreeTagJumps()

    enterCommand("3pop")

    assertPosition(0, 8)
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test pop with count moves the marker in the tag stack`() {
    configureWithThreeTagJumps()

    enterCommand("2pop")

    enterCommand("tags")
    assertExOutput(
      """  # TO tag         FROM line  in file/text
        |  1  1 it                  1  I found it in a legendary land
        |> 2  1 all                 2  all rocks and lavender and tufted grass,
        |  3  1 where               3  where it was settled on some sodden sand
      """.trimMargin(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test pop past the bottom of the stack reports error`() {
    configureWithThreeTagJumps()

    enterCommand("4pop")

    assertPluginError(true)
    assertPluginErrorMessage("E555: At bottom of tag stack")
    assertPosition(3, 0)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test pop with empty tag stack reports error`() {
    configureByText(text)

    enterCommand("pop")

    assertPluginError(true)
    assertPluginErrorMessage("E73: Tag stack empty")
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test po is an abbreviation for pop`() {
    configureWithThreeTagJumps()

    enterCommand("po")

    assertPluginError(false)
    assertPosition(2, 0)
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test pop records the position it left in the jump list`() {
    configureWithThreeTagJumps()

    enterCommand("pop")
    assertPosition(2, 0)

    typeText("<C-O>")
    assertPosition(3, 0)
  }
}
