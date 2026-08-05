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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

/**
 * Tests for the `:YRShow` command, which lists the contents of the yank ring (VIM-301).
 *
 * The yank ring is a port of YankRing.vim, so there is nothing to compare against in Neovim.
 *
 * YankRing renders the list into a separate window when `g:yankring_window_use_separate` is set,
 * and echoes it otherwise. We implement the echo variant first; the window comes later.
 */
class YRShowCommandTest : VimTestCase() {

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("yankring")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test show yanked text`() {
    configureByText("${c}Lorem ipsum")
    enterCommand("YRClear")
    typeText("yiw")
    enterCommand("YRShow")
    assertExOutput(
      """--- YankRing ---
      |Elem  Content
      |1     Lorem
      """.trimMargin(),
    )
  }

  // A2
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test YRClear empties the ring`() {
    configureByText("${c}Lorem ipsum")
    typeText("yiw")
    enterCommand("YRClear")
    enterCommand("YRShow")
    assertExOutput(
      """--- YankRing ---
      |Elem  Content
      """.trimMargin(),
    )
  }

  // A3
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test entries are listed newest first`() {
    configureByText("${c}one two three")
    enterCommand("YRClear")
    typeText("yiw")
    typeText("wyiw")
    typeText("wyiw")
    enterCommand("YRShow")
    assertExOutput(
      """--- YankRing ---
      |Elem  Content
      |1     three
      |2     two
      |3     one
      """.trimMargin(),
    )
  }

  // A4
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test delete feeds the ring`() {
    configureByText("${c}Lorem ipsum")
    enterCommand("YRClear")
    typeText("diw")
    enterCommand("YRShow")
    assertExOutput(
      """--- YankRing ---
      |Elem  Content
      |1     Lorem
      """.trimMargin(),
    )
  }

  // A4
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test single character delete feeds the ring`() {
    configureByText("${c}Lorem ipsum")
    enterCommand("YRClear")
    typeText("x")
    enterCommand("YRShow")
    assertExOutput(
      """--- YankRing ---
      |Elem  Content
      |1     L
      """.trimMargin(),
    )
  }

  // A4
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test change feeds the ring`() {
    configureByText("${c}Lorem ipsum")
    enterCommand("YRClear")
    typeText("ciw<Esc>")
    enterCommand("YRShow")
    assertExOutput(
      """--- YankRing ---
      |Elem  Content
      |1     Lorem
      """.trimMargin(),
    )
  }

  // A5
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test newlines are shown as literal backslash n`() {
    configureByText("${c}Lorem\nipsum")
    enterCommand("YRClear")
    typeText("yy")
    enterCommand("YRShow")
    assertExOutput(
      """--- YankRing ---
      |Elem  Content
      |1     Lorem\n
      """.trimMargin(),
    )
  }

  // A6
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test non printable characters are encoded`() {
    configureByText("${c}a\tb")
    enterCommand("YRClear")
    typeText("y$")
    enterCommand("YRShow")
    assertExOutput(
      """--- YankRing ---
      |Elem  Content
      |1     a^Ib
      """.trimMargin(),
    )
  }

  // A7
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test duplicate moves to the top instead of being appended`() {
    configureByText("${c}one two one")
    enterCommand("YRClear")
    typeText("yiw")
    typeText("wyiw")
    typeText("wyiw")
    enterCommand("YRShow")
    assertExOutput(
      """--- YankRing ---
      |Elem  Content
      |1     one
      |2     two
      """.trimMargin(),
    )
  }

  // A8
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test black hole register does not feed the ring`() {
    configureByText("${c}Lorem ipsum")
    enterCommand("YRClear")
    typeText("yiw")
    typeText("w\"_diw")
    enterCommand("YRShow")
    assertExOutput(
      """--- YankRing ---
      |Elem  Content
      |1     Lorem
      """.trimMargin(),
    )
  }

  // A9
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test explicit register feeds the ring as well as the register`() {
    configureByText("${c}Lorem ipsum")
    enterCommand("YRClear")
    typeText("\"ayiw")

    enterCommand("YRShow")
    assertExOutput(
      """--- YankRing ---
      |Elem  Content
      |1     Lorem
      """.trimMargin(),
    )

    enterCommand("registers a")
    assertExOutput(
      """Type Name Content
      |  c  "a   Lorem
      """.trimMargin(),
    )
  }

  // A10
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test macro recording does not pollute the ring`() {
    configureByText("${c}Lorem ipsum")
    enterCommand("YRClear")
    typeText("qawq")
    enterCommand("YRShow")
    assertExOutput(
      """--- YankRing ---
      |Elem  Content
      """.trimMargin(),
    )
  }
}
