/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.yankring

import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

/**
 * Tests for the yank ring's configuration variables (VIM-301, group D).
 *
 * Configuration uses the plugin's own `g:yankring_*` globals rather than `:set` options, for the
 * same reason the commands keep their names.
 */
class YankRingOptionsTest : VimTestCase() {

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("yankring")
  }

  // D1 / A11
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test max history caps the ring and drops the oldest entry`() {
    configureByText("${c}one two three")
    enterCommand("YRClear")
    enterCommand("let g:yankring_max_history = 2")

    typeText("yiw")
    typeText("wyiw")
    typeText("wyiw")

    enterCommand("YRShow")
    assertExOutput(
      """--- YankRing ---
      |Elem  Content
      |1     three
      |2     two
      """.trimMargin(),
    )
  }

  /**
   * The cap is read on every add rather than captured once, so lowering it mid-session takes effect
   * instead of waiting for a restart.
   */
  // D1
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test lowering max history trims the ring on the next yank`() {
    configureByText("${c}one two three four")
    enterCommand("YRClear")
    enterCommand("let g:yankring_max_history = 10")

    typeText("yiw")
    typeText("wyiw")
    typeText("wyiw")

    enterCommand("let g:yankring_max_history = 2")
    typeText("wyiw")

    enterCommand("YRShow")
    assertExOutput(
      """--- YankRing ---
      |Elem  Content
      |1     four
      |2     three
      """.trimMargin(),
    )
  }

  /**
   * Guards the default. Yanking a hundred times to prove it is exactly 100 is not worth the test
   * time, but a handful of entries surviving proves it did not quietly become tiny.
   */
  // D1
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test the ring keeps several entries without any configuration`() {
    configureByText("${c}one two three four five")
    enterCommand("YRClear")

    typeText("yiw")
    typeText("wyiw")
    typeText("wyiw")
    typeText("wyiw")
    typeText("wyiw")

    enterCommand("YRShow")
    assertExOutput(
      """--- YankRing ---
      |Elem  Content
      |1     five
      |2     four
      |3     three
      |4     two
      |5     one
      """.trimMargin(),
    )
  }
}
