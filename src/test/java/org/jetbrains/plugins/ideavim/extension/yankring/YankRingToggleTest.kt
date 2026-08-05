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
 * Turning the port off has to leave no trace behind (VIM-301, A12).
 *
 * `set yankring` and `Plug 'YankRing'` are the same switch - `PlugCommand` resolves the alias and
 * flips the very `ToggleOption` that `set` flips - so `set noyankring` is what undoes either.
 *
 * The plugin itself has no enable switch at all: a Vim plugin in `plugin/` is simply always loaded.
 * `g:yankring_enabled` and `:YRToggle` are its *runtime* switch, and they are E3, not this.
 */
class YankRingToggleTest : VimTestCase() {

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("yankring")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test disabling stops recording`() {
    configureByText("${c}one two")
    enterCommand("YRClear")
    typeText("yiw")

    enterCommand("set noyankring")
    typeText("wyiw")

    enterCommand("set yankring")
    enterCommand("YRShow")
    assertExOutput(
      """--- YankRing ---
      |Elem  Content
      |1     one
      """.trimMargin(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test disabling gives the paste keys and ctrl-p back`() {
    configureByText("one\n${c}two")
    enterCommand("set noyankring")

    typeText("<C-P>")
    assertState("${c}one\ntwo")
    assertPluginError(false)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test disabling removes the commands`() {
    configureByText("${c}one two")
    enterCommand("set noyankring")

    enterCommand("YRShow")
    assertPluginError(true)
  }

  /**
   * The recorder is a singleton, so a disable that forgot to unregister it would leave two copies
   * registered after re-enabling and every yank would be recorded twice.
   */
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN)
  @Test
  fun `test re-enabling does not record twice`() {
    configureByText("${c}one two")
    enterCommand("set noyankring")
    enterCommand("set yankring")

    enterCommand("YRClear")
    typeText("yiw")

    enterCommand("YRShow")
    assertExOutput(
      """--- YankRing ---
      |Elem  Content
      |1     one
      """.trimMargin(),
    )
  }
}
