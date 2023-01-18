/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.option

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.options.StringOption
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class StringListOptionTest : VimTestCase() {
  private val optionName = "myOpt"

  override fun setUp() {
    super.setUp()
    injector.optionService.addOption(StringOption(optionName, optionName, "", true, null))
  }

  override fun tearDown() {
    super.tearDown()
    injector.optionService.removeOption(optionName)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test append existing value`() {
    injector.optionService.appendValue(OptionScope.GLOBAL, optionName, "123")
    injector.optionService.appendValue(OptionScope.GLOBAL, optionName, "456")
    injector.optionService.appendValue(OptionScope.GLOBAL, optionName, "123")

    assertEquals("123,456", optionsNoEditor().getStringValue(optionName))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test prepend existing value`() {
    injector.optionService.appendValue(OptionScope.GLOBAL, optionName, "456")
    injector.optionService.appendValue(OptionScope.GLOBAL, optionName, "123")
    injector.optionService.prependValue(OptionScope.GLOBAL, optionName, "123")

    assertEquals("456,123", optionsNoEditor().getStringValue(optionName))
  }
}
