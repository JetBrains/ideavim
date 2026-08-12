/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

/**
 * Parsing and validation tests for `:stopinsert`
 *
 * See `:help :stopinsert`.
 */
class StopInsertCommandTest : VimTestCase() {

  @Test
  fun `test stopinsert in normal mode is a no-op`() {
    configureByText("abc<caret>def")
    enterCommand("stopinsert")

    assertPluginError(false)
    assertMode(Mode.NORMAL())
    assertState("abc<caret>def")
  }

  @Test
  fun `test stopi abbreviation is accepted`() {
    configureByText("abc<caret>def")
    enterCommand("stopi")

    assertPluginError(false)
    assertMode(Mode.NORMAL())
  }

  @Test
  fun `test bang is accepted`() {
    configureByText("abc<caret>def")
    enterCommand("stopinsert!")

    assertPluginError(false)
    assertMode(Mode.NORMAL())
  }

  @Test
  fun `test trailing characters raises an error`() {
    configureByText("abc<caret>def")
    enterCommand("stopinsert foo")

    assertPluginError(true)
    assertPluginErrorMessage("E488: Trailing characters: foo")
  }

  @Test
  fun `test range raises an error`() {
    configureByText("abc<caret>def")
    enterCommand("1stopinsert")

    assertPluginError(true)
    assertPluginErrorMessage("E481: No range allowed")
  }
}
