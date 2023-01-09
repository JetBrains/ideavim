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

class InsertTabActionTest : VimTestCase() {
  fun `test insert tab`() {
    setupChecks {
      keyHandler = Checks.KeyHandlerMethod.DIRECT_TO_VIM
    }
    val before = "I fo${c}und it in a legendary land"
    val after = "I fo    ${c}und it in a legendary land"
    configureByText(before)

    typeText(injector.parser.parseKeys("i" + "<Tab>"))

    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  fun `test insert tab scrolls at end of line`() {
    setupChecks {
      keyHandler = Checks.KeyHandlerMethod.DIRECT_TO_VIM
    }
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.sidescrolloff, VimInt(10))
    configureByColumns(200)

    // TODO: This works for tests, but not in real life. See VimShortcutKeyAction.isEnabled
    typeText(injector.parser.parseKeys("70|" + "i" + "<Tab>"))
    assertVisibleLineBounds(0, 32, 111)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  fun `test insert tab scrolls at end of line 2`() {
    VimPlugin.getOptionService().setOptionValue(OptionScope.GLOBAL, OptionConstants.sidescrolloff, VimInt(10))
    configureByColumns(200)
    typeText(injector.parser.parseKeys("70|" + "i" + "<C-I>"))
    assertVisibleLineBounds(0, 32, 111)
  }
}
