/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.mark

import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

/**
 * Tests for the tag stack, see "h tag-stack"
 *
 * Step 1 of VIM-1370: `<C-T>` is its own command, not an alias of `<C-O>`. It walks the tag stack, and with an empty
 * stack it fails with E73 instead of falling back to the jump list.
 */
class TagStackTest : VimTestCase() {
  private val text = """
        I found ${c}it in a legendary land
        all rocks and lavender and tufted grass,
        where it was settled on some sodden sand
        hard by the torrent of a mountain pass.
  """.trimIndent()

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test ctrl-t with empty tag stack reports error`() {
    configureByText(text)
    typeText("<C-T>")
    assertPluginError(true)
    assertPluginErrorMessage("E73: tag stack empty")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test ctrl-t with count and empty tag stack reports error`() {
    configureByText(text)
    typeText("2<C-T>")
    assertPluginError(true)
    assertPluginErrorMessage("E73: tag stack empty")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test ctrl-t with empty tag stack does not move caret`() {
    configureByText(text)
    enterSearch("sodden")
    assertPosition(2, 29)

    typeText("<C-T>")

    // The jump list holds the position we searched from, but the tag stack is empty - <C-T> must not use it
    assertPosition(2, 29)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test ctrl-t with empty tag stack does not consume the jump list`() {
    configureByText(text)
    enterSearch("sodden")
    enterSearch("torrent")
    assertPosition(3, 12)

    typeText("<C-T>")
    typeText("<C-O>")

    // The failed <C-T> must not have advanced the jump list, so this is the first step back, not the second
    assertPosition(2, 29)
  }

  @Test
  fun `test ctrl-o still walks the jump list`() {
    configureByText(text)
    enterSearch("sodden")

    typeText("<C-O>")

    assertPosition(0, 8)
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test ctrl-bracket records the position it was made from`() {
    configureByText(text)
    typeText("<C-]>")

    // Not a tag jump, so the stack is untouched by it
    typeText("G")

    typeText("<C-T>")
    assertPluginError(false)
    assertPosition(0, 8)
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test ctrl-t walks back through several tag jumps`() {
    configureByText(text)
    typeText("<C-]>")
    typeText("j0")
    typeText("<C-]>")
    typeText("G")

    typeText("<C-T>")
    assertPosition(1, 0)

    typeText("<C-T>")
    assertPosition(0, 8)
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test ctrl-t at the bottom of the tag stack reports error`() {
    configureByText(text)
    typeText("<C-]>")
    typeText("G")

    typeText("<C-T>")
    assertPluginError(false)

    typeText("<C-T>")
    assertPluginError(true)
    assertPluginErrorMessage("E555: at bottom of tag stack")
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test ctrl-bracket also adds to the jump list`() {
    configureByText(text)
    typeText("<C-]>")

    // A plain motion, so <C-O> can only work if <C-]> recorded a jump of its own
    typeText("j")

    typeText("<C-O>")
    assertPosition(0, 8)
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test gd does not touch the tag stack`() {
    configureByText(text)

    // gd is "goto local declaration", not a tag command - in Vim it does not use the tag stack
    typeText("gd")
    typeText("G")

    typeText("<C-T>")
    assertPluginError(true)
    assertPluginErrorMessage("E73: tag stack empty")
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test ctrl-bracket with no identifier on the line does not push`() {
    configureByText(
      """
        ${c}
        all rocks and lavender and tufted grass,
      """.trimIndent(),
    )
    typeText("<C-]>")
    typeText("G")

    typeText("<C-T>")
    assertPluginError(true)
    assertPluginErrorMessage("E73: tag stack empty")
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test ctrl-t with count walks several entries back`() {
    configureByText(text)
    typeText("<C-]>")
    typeText("j0")
    typeText("<C-]>")
    typeText("j0")
    typeText("<C-]>")
    typeText("G")

    typeText("2<C-T>")

    assertPluginError(false)
    assertPosition(1, 0)
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test ctrl-t with count past the bottom reports error`() {
    configureByText(text)
    typeText("<C-]>")
    typeText("G")

    typeText("3<C-T>")

    assertPluginError(true)
    assertPluginErrorMessage("E555: at bottom of tag stack")
  }

  /**
   * Leaves three entries on the stack, jumped from (0, 8), (1, 0) and (2, 0)
   */
  private fun configureWithThreeTagJumps() {
    configureByText(text)
    typeText("<C-]>")
    typeText("j0")
    typeText("<C-]>")
    typeText("j0")
    typeText("<C-]>")
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test a new tag jump drops the entries walked back past`() {
    configureWithThreeTagJumps()
    typeText("2<C-T>")

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
  fun `test walking back cannot reach a dropped entry`() {
    configureWithThreeTagJumps()
    typeText("2<C-T>")
    typeText("<C-]>")
    typeText("G")

    typeText("<C-T>")
    assertPosition(1, 0)

    typeText("<C-T>")
    assertPosition(0, 8)

    typeText("<C-T>")
    assertPluginError(true)
    assertPluginErrorMessage("E555: at bottom of tag stack")
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test a tag jump from the bottom of the stack drops everything above it`() {
    configureWithThreeTagJumps()
    typeText("3<C-T>")

    typeText("<C-]>")

    enterCommand("tags")
    assertExOutput(
      """  # TO tag         FROM line  in file/text
        |  1  1 it                  1  I found it in a legendary land
        |>
      """.trimMargin(),
    )
  }
}
