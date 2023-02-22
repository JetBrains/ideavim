/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.option

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.options.StringOption
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class StringListOptionTest : VimTestCase() {
  private val optionName = "myOpt"

  override fun setUp() {
    super.setUp()
    injector.optionGroup.addOption(StringOption(optionName, optionName, "", true, null))
    configureByText("\n")
  }

  override fun tearDown() {
    super.tearDown()
    injector.optionGroup.removeOption(optionName)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test append existing value`() {
    enterCommand("set $optionName+=123")
    enterCommand("set $optionName+=456")
    enterCommand("set $optionName+=123")

    assertEquals("123,456", options().getStringValue(optionName))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test prepend existing value`() {
    enterCommand("set $optionName+=456")
    enterCommand("set $optionName+=123")
    enterCommand("set $optionName^=123")

    assertEquals("456,123", options().getStringValue(optionName))
  }
}
