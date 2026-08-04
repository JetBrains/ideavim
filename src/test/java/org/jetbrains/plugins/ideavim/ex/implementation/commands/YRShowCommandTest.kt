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
    enableExtensionsNewApi("YankRing")
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
}
