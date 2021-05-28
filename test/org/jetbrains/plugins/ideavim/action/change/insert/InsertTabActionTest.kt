/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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

package org.jetbrains.plugins.ideavim.action.change.insert

import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.option.OptionsManager
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class InsertTabActionTest : VimTestCase() {
  fun `test insert tab`() {
      val before = "I fo${c}und it in a legendary land"
      val after = "I fo    ${c}und it in a legendary land"
      configureByText(before)

      typeText(parseKeys("i", "<Tab>"))

      assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  fun `test insert tab scrolls at end of line`() {
    OptionsManager.sidescrolloff.set(10)
    configureByColumns(200)

    // TODO: This works for tests, but not in real life. See VimShortcutKeyAction.isEnabled
    typeText(parseKeys("70|", "i", "<Tab>"))
    assertVisibleLineBounds(0, 32, 111)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  fun `test insert tab scrolls at end of line 2`() {
    OptionsManager.sidescrolloff.set(10)
    configureByColumns(200)
    typeText(parseKeys("70|", "i", "<C-I>"))
    assertVisibleLineBounds(0, 32, 111)
  }
}
