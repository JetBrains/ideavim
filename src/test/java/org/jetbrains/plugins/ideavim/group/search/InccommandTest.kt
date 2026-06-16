/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.group.search

import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

@Suppress("SpellCheckingInspection")
class InccommandTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test inccommand nosplit highlights replaced text`() {
    configureByText(
      """My name is Cezary Baryka, and for the last 20 minutes I have been the owner of this glass house.
         |${c}I'm slowly starting to regret the purchase, freezing cold at night, sweltering heat by day,
         |zero ventilation and no plumbing are doing their job. That's right, it stinks.
         |Ehhh..... I lied.... I didn't buy this pigsty, I won it from my father in a game of cards:
      """.trimMargin(),
    )
    enterCommand("set inccommand=nosplit")

    typeText(":", "%s/the/X/g")

    // The replacement text in the previewed buffer should be highlighted so the user can see what changed.
    assertSearchHighlights(
      "the",
      """My name is Cezary Baryka, and for «X» last 20 minutes I have been «X» owner of this glass house.
         |I'm slowly starting to regret «X» purchase, freezing cold at night, sweltering heat by day,
         |zero ventilation and no plumbing are doing «X»ir job. That's right, it stinks.
         |Ehhh..... I lied.... I didn't buy this pigsty, I won it from my fa«X»r in a game of cards:
      """.trimMargin(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test inccommand nosplit previews replacement in whole file`() {
    configureByText(
      """My name is Cezary Baryka, and for the last 20 minutes I have been the owner of this glass house.
         |${c}I'm slowly starting to regret the purchase, freezing cold at night, sweltering heat by day,
         |zero ventilation and no plumbing are doing their job. That's right, it stinks.
         |Ehhh..... I lied.... I didn't buy this pigsty, I won it from my father in a game of cards:
      """.trimMargin(),
    )
    enterCommand("set inccommand=nosplit")

    // No <CR> - the preview should be visible while still typing the command
    typeText(":", "%s/the/X/g")

    assertState(
      """My name is Cezary Baryka, and for X last 20 minutes I have been X owner of this glass house.
         |I'm slowly starting to regret X purchase, freezing cold at night, sweltering heat by day,
         |zero ventilation and no plumbing are doing Xir job. That's right, it stinks.
         |Ehhh..... I lied.... I didn't buy this pigsty, I won it from my faXr in a game of cards:
      """.trimMargin(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test inccommand nosplit previews replacement in current line with no range`() {
    configureByText(
      """My name is Cezary Baryka, and for the last 20 minutes I have been the owner of this glass house.
         |${c}I'm slowly starting to regret the purchase, freezing cold at night, sweltering heat by day,
         |zero ventilation and no plumbing are doing their job. That's right, it stinks.
         |Ehhh..... I lied.... I didn't buy this pigsty, I won it from my father in a game of cards:
      """.trimMargin(),
    )
    enterCommand("set inccommand=nosplit")

    typeText(":", "s/the/X/g")

    assertState(
      """My name is Cezary Baryka, and for the last 20 minutes I have been the owner of this glass house.
         |I'm slowly starting to regret X purchase, freezing cold at night, sweltering heat by day,
         |zero ventilation and no plumbing are doing their job. That's right, it stinks.
         |Ehhh..... I lied.... I didn't buy this pigsty, I won it from my father in a game of cards:
      """.trimMargin(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test inccommand nosplit previews replacement only in range`() {
    configureByText(
      """My name is Cezary Baryka, and for the last 20 minutes I have been the owner of this glass house.
         |I'm slowly starting to regret the purchase, freezing cold at night, sweltering heat by day,
         |zero ventilation and no plumbing are doing their job. That's right, it stinks.
         |${c}Ehhh..... I lied.... I didn't buy this pigsty, I won it from my father in a game of cards:
      """.trimMargin(),
    )
    enterCommand("set inccommand=nosplit")

    typeText(":", "2,3s/the/X/g")

    assertState(
      """My name is Cezary Baryka, and for the last 20 minutes I have been the owner of this glass house.
         |I'm slowly starting to regret X purchase, freezing cold at night, sweltering heat by day,
         |zero ventilation and no plumbing are doing Xir job. That's right, it stinks.
         |Ehhh..... I lied.... I didn't buy this pigsty, I won it from my father in a game of cards:
      """.trimMargin(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test inccommand nosplit updates preview after each key press`() {
    configureByText(
      """My name is Cezary Baryka, and for the last 20 minutes I have been the owner of this glass house.
         |${c}I'm slowly starting to regret the purchase, freezing cold at night, sweltering heat by day,
         |zero ventilation and no plumbing are doing their job. That's right, it stinks.
         |Ehhh..... I lied.... I didn't buy this pigsty, I won it from my father in a game of cards:
      """.trimMargin(),
    )
    enterCommand("set inccommand=nosplit")

    // Without the /g flag only the first match per line is previewed (note line 1's second "the" is left untouched)
    typeText(":", "%s/the/X")
    assertState(
      """My name is Cezary Baryka, and for X last 20 minutes I have been the owner of this glass house.
         |I'm slowly starting to regret X purchase, freezing cold at night, sweltering heat by day,
         |zero ventilation and no plumbing are doing Xir job. That's right, it stinks.
         |Ehhh..... I lied.... I didn't buy this pigsty, I won it from my faXr in a game of cards:
      """.trimMargin(),
    )

    // Continue typing - the preview should follow the new replacement text
    typeText("YZ")
    assertState(
      """My name is Cezary Baryka, and for XYZ last 20 minutes I have been the owner of this glass house.
         |I'm slowly starting to regret XYZ purchase, freezing cold at night, sweltering heat by day,
         |zero ventilation and no plumbing are doing XYZir job. That's right, it stinks.
         |Ehhh..... I lied.... I didn't buy this pigsty, I won it from my faXYZr in a game of cards:
      """.trimMargin(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test inccommand nosplit restores original text on escape`() {
    val original =
      """My name is Cezary Baryka, and for the last 20 minutes I have been the owner of this glass house.
         |${c}I'm slowly starting to regret the purchase, freezing cold at night, sweltering heat by day,
         |zero ventilation and no plumbing are doing their job. That's right, it stinks.
         |Ehhh..... I lied.... I didn't buy this pigsty, I won it from my father in a game of cards:
      """.trimMargin()
    configureByText(original)
    enterCommand("set inccommand=nosplit")

    typeText(":", "%s/the/X/g", "<Esc>")

    assertState(original)
    // Cancelling must also remove the preview highlights.
    assertNoSearchHighlights()
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test inccommand nosplit applies replacement on enter`() {
    configureByText(
      """My name is Cezary Baryka, and for the last 20 minutes I have been the owner of this glass house.
         |${c}I'm slowly starting to regret the purchase, freezing cold at night, sweltering heat by day,
         |zero ventilation and no plumbing are doing their job. That's right, it stinks.
         |Ehhh..... I lied.... I didn't buy this pigsty, I won it from my father in a game of cards:
      """.trimMargin(),
    )
    enterCommand("set inccommand=nosplit")

    typeText(":", "%s/the/X/g", "<CR>")

    assertState(
      """My name is Cezary Baryka, and for X last 20 minutes I have been X owner of this glass house.
         |I'm slowly starting to regret X purchase, freezing cold at night, sweltering heat by day,
         |zero ventilation and no plumbing are doing Xir job. That's right, it stinks.
         |Ehhh..... I lied.... I didn't buy this pigsty, I won it from my faXr in a game of cards:
      """.trimMargin(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test inccommand empty value does not preview replacement`() {
    val original =
      """My name is Cezary Baryka, and for the last 20 minutes I have been the owner of this glass house.
         |${c}I'm slowly starting to regret the purchase, freezing cold at night, sweltering heat by day,
         |zero ventilation and no plumbing are doing their job. That's right, it stinks.
         |Ehhh..... I lied.... I didn't buy this pigsty, I won it from my father in a game of cards:
      """.trimMargin()
    configureByText(original)
    enterCommand("set inccommand=")

    // With no inccommand preview, the document is unchanged while typing
    typeText(":", "%s/the/X/g")

    assertState(original)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `should not replace until pass replaced test`() {
    val original =
      """My name is Cezary Baryka, and for the last 20 minutes I have been the owner of this glass house.
         |${c}I'm slowly starting to regret the purchase, freezing cold at night, sweltering heat by day,
         |zero ventilation and no plumbing are doing their job. That's right, it stinks.
         |Ehhh..... I lied.... I didn't buy this pigsty, I won it from my father in a game of cards:
      """.trimMargin()
    configureByText(original)
    enterCommand("set inccommand=nosplit")

    typeText(":", "%s/the")

    assertState(original)
  }
}
