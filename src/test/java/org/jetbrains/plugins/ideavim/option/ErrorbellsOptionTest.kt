/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package org.jetbrains.plugins.ideavim.option

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.options
import com.maddyhome.idea.vim.newapi.vim
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// VIM-485 |'errorbells'| |'noerrorbells'|
@TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
class ErrorbellsOptionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  private fun errorbells() = injector.options(fixture.editor.vim).errorbells

  @Test
  fun `test noerrorbells is the default, as in Vim`() {
    assertFalse(errorbells())
  }

  @Test
  fun `test noerrorbells is accepted`() {
    enterCommand("set noerrorbells")
    assertPluginError(false)
    assertFalse(errorbells())
  }

  @Test
  fun `test errorbells can be re-enabled`() {
    enterCommand("set noerrorbells")
    enterCommand("set errorbells")
    assertPluginError(false)
    assertTrue(errorbells())
  }

  @Test
  fun `test noerrorbells abbreviation is accepted`() {
    enterCommand("set noeb")
    assertPluginError(false)
    assertFalse(errorbells())
  }
}
