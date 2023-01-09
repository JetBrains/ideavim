/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.insert

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class InsertBackspaceActionTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  fun `test insert backspace`() {
    val before = "I fo${c}und it in a legendary land"
    val after = "I f${c}und it in a legendary land"
    configureByText(before)

    typeText(injector.parser.parseKeys("i" + "<BS>"))

    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  fun `test insert backspace scrolls start of line`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.sidescrolloff, VimInt(10))
    configureByColumns(200)

    typeText(injector.parser.parseKeys("70zl" + "i" + "<BS>"))
    assertVisibleLineBounds(0, 39, 118)
  }
}
