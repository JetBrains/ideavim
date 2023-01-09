/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.expressions

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class OptionTest : VimTestCase() {

  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test option`() {
    configureByText("\n")
    VimPlugin.getOptionService().setOption(OptionScope.GLOBAL, OptionConstants.ignorecase)
    typeText(commandToKeys("if &ic | echo 'ignore case is on' | else | echo 'ignore case is off' | endif"))
    assertExOutput("ignore case is on\n")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test option2`() {
    configureByText("\n")
    VimPlugin.getOptionService().unsetOption(OptionScope.GLOBAL, OptionConstants.ignorecase)
    typeText(commandToKeys("if &ic | echo 'ignore case is on' | else | echo 'ignore case is off' | endif"))
    assertExOutput("ignore case is off\n")
  }
}
