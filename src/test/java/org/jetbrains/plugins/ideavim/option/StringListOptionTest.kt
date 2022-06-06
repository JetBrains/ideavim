/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package org.jetbrains.plugins.ideavim.option

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.options.StringOption
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class StringListOptionTest : VimTestCase() {
  private val optionName = "myOpt"

  init {
    injector.optionService.addOption(StringOption(optionName, optionName, "", true, null))
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test append existing value`() {
    injector.optionService.appendValue(OptionScope.GLOBAL, optionName, "123")
    injector.optionService.appendValue(OptionScope.GLOBAL, optionName, "456")
    injector.optionService.appendValue(OptionScope.GLOBAL, optionName, "123")

    assertEquals("123,456", (injector.optionService.getOptionValue(OptionScope.GLOBAL, optionName) as VimString).value)
    injector.optionService.resetDefault(OptionScope.GLOBAL, optionName)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test prepend existing value`() {
    injector.optionService.appendValue(OptionScope.GLOBAL, optionName, "456")
    injector.optionService.appendValue(OptionScope.GLOBAL, optionName, "123")
    injector.optionService.prependValue(OptionScope.GLOBAL, optionName, "123")

    assertEquals("456,123", (injector.optionService.getOptionValue(OptionScope.GLOBAL, optionName) as VimString).value)
    injector.optionService.resetDefault(OptionScope.GLOBAL, optionName)
  }
}
