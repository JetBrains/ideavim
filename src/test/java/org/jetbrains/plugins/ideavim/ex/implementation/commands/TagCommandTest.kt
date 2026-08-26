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
 * Tests for `:tag` without an argument, see "h :tag"
 *
 * `:[count]tag` walks [count] entries back down the tag stack, the opposite of `:pop`. Error texts and the movement of
 * the marker were taken from Vim 9.1.
 *
 * These tests watch the marker in `:tags` rather than the caret. Walking down means redoing a tag lookup, and in a
 * plain text fixture the IDE has no declaration to navigate to, so the caret alone cannot tell the cases apart.
 */
class TagCommandTest : VimTestCase() {
  private val text = """
        I found ${c}it in a legendary land
        all rocks and lavender and tufted grass,
        where it was settled on some sodden sand
        hard by the torrent of a mountain pass.
  """.trimIndent()

  /**
   * Leaves two entries on the stack and walks all the way back up, so the marker sits on the first entry
   */
  private fun configureAtBottomOfStack() {
    configureByText(text)
    typeText("<C-]>")
    typeText("j0")
    typeText("<C-]>")
    typeText("2<C-T>")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test tag with empty stack reports error`() {
    configureByText(text)

    enterCommand("tag")

    assertPluginError(true)
    assertPluginErrorMessage("E73: Tag stack empty")
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test tag at the top of the stack reports error`() {
    configureByText(text)
    typeText("<C-]>")

    enterCommand("tag")

    assertPluginError(true)
    assertPluginErrorMessage("E556: At top of tag stack")
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test tag walks one entry back down`() {
    configureAtBottomOfStack()

    enterCommand("tag")

    assertPluginError(false)
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
  fun `test tag walks back down to the top of the stack`() {
    configureAtBottomOfStack()

    enterCommand("tag")
    enterCommand("tag")

    // Reaching the top is a successful walk down, not a failure
    assertPluginError(false)
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
  fun `test tag with count walks several entries back down`() {
    configureAtBottomOfStack()

    enterCommand("2tag")

    assertPluginError(false)
    enterCommand("tags")
    // The second entry was redone from line 1, so that is its FROM now - verified against Vim 9.1
    assertExOutput(
      """  # TO tag         FROM line  in file/text
        |  1  1 it                  1  I found it in a legendary land
        |  2  1 all                 1  I found it in a legendary land
        |>
      """.trimMargin(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test tag past the top of the stack reports error`() {
    configureAtBottomOfStack()

    enterCommand("3tag")

    assertPluginError(true)
    assertPluginErrorMessage("E556: At top of tag stack")
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test tag and pop walk the stack in opposite directions`() {
    configureAtBottomOfStack()

    enterCommand("tag")
    enterCommand("pop")

    enterCommand("tags")
    assertExOutput(
      """  # TO tag         FROM line  in file/text
        |> 1  1 it                  1  I found it in a legendary land
        |  2  1 all                 2  all rocks and lavender and tufted grass,
      """.trimMargin(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test ta is an abbreviation for tag`() {
    configureAtBottomOfStack()

    enterCommand("ta")

    assertPluginError(false)
    enterCommand("tags")
    assertExOutput(
      """  # TO tag         FROM line  in file/text
        |  1  1 it                  1  I found it in a legendary land
        |> 2  1 all                 2  all rocks and lavender and tufted grass,
      """.trimMargin(),
    )
  }

  /**
   * Leaves three entries on the stack, jumped from (0, 8), (1, 0) and (2, 0), and walks all the way back up
   */
  private fun configureAtBottomOfThreeEntryStack() {
    configureByText(text)
    typeText("<C-]>")
    typeText("j0")
    typeText("<C-]>")
    typeText("j0")
    typeText("<C-]>")
    typeText("3<C-T>")
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test tag moves to where the tag jump landed`() {
    configureAtBottomOfStack()
    assertPosition(0, 8)

    enterCommand("tag")

    // Walking down redoes a tag jump, so it lands where that jump ended - which is where the next one was made from,
    // not where this one started
    assertPosition(1, 0)
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test tag walks down through the middle of the stack`() {
    configureAtBottomOfThreeEntryStack()
    assertPosition(0, 8)

    enterCommand("tag")
    assertPosition(1, 0)

    enterCommand("tag")
    assertPosition(2, 0)
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test tag with count moves to where that tag jump landed`() {
    configureAtBottomOfThreeEntryStack()

    enterCommand("2tag")

    assertPosition(2, 0)
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test tag records where the jump was redone from`() {
    configureAtBottomOfStack()
    typeText("G")

    enterCommand("tag")

    // Redoing a tag jump records where it was redone from, so the first entry no longer points at line 1
    enterCommand("tags")
    assertExOutput(
      """  # TO tag         FROM line  in file/text
        |  1  1 it                  4  hard by the torrent of a mountain pass.
        |> 2  1 all                 2  all rocks and lavender and tufted grass,
      """.trimMargin(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test ctrl-t comes back to where the jump was redone from`() {
    configureAtBottomOfStack()
    typeText("G")
    enterCommand("tag")
    assertPosition(1, 0)

    typeText("<C-T>")

    // The whole point of rewriting the entry - we came from the last line, so that is where we go back to
    assertPosition(3, 0)
  }
}
