/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.option

import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

/**
 * Tests for the 'tagstack' option, see "h 'tagstack'"
 *
 * Verified against Vim 9.1: with the option off a tag jump still navigates, it just records nothing. Walking the stack
 * is unaffected - `<C-T>` keeps using whatever is already on it, and keeps moving the marker.
 */
class TagStackOptionTest : VimTestCase() {
  private val text = """
        I found ${c}it in a legendary land
        all rocks and lavender and tufted grass,
        where it was settled on some sodden sand
        hard by the torrent of a mountain pass.
  """.trimIndent()

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test tagstack is on by default`() {
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
  fun `test notagstack records nothing`() {
    configureByText(text)
    enterCommand("set notagstack")

    typeText("<C-]>")

    enterCommand("tags")
    assertExOutput(
      """  # TO tag         FROM line  in file/text
        |>
      """.trimMargin(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test notagstack leaves nothing to walk back over`() {
    configureByText(text)
    enterCommand("set notagstack")
    typeText("<C-]>")
    typeText("G")

    typeText("<C-T>")

    assertPluginError(true)
    assertPluginErrorMessage("E73: Tag stack empty")
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test notagstack still walks an existing stack`() {
    configureByText(text)
    typeText("<C-]>")
    enterCommand("set notagstack")
    typeText("G")

    typeText("<C-T>")

    assertPluginError(false)
    assertPosition(0, 8)
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test turning tagstack back on resumes recording`() {
    configureByText(text)
    enterCommand("set notagstack")
    typeText("<C-]>")
    enterCommand("set tagstack")
    typeText("j0")

    typeText("<C-]>")

    enterCommand("tags")
    assertExOutput(
      """  # TO tag         FROM line  in file/text
        |  1  1 all                 2  all rocks and lavender and tufted grass,
        |>
      """.trimMargin(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.ACTION_COMMAND)
  @Test
  fun `test toggling the option does not clear the stack`() {
    configureByText(text)
    typeText("<C-]>")

    enterCommand("set notagstack")
    enterCommand("set tagstack")

    // Vim leaves the stack untouched - the option only decides whether new jumps are recorded
    enterCommand("tags")
    assertExOutput(
      """  # TO tag         FROM line  in file/text
        |  1  1 it                  1  I found it in a legendary land
        |>
      """.trimMargin(),
    )
  }
}
