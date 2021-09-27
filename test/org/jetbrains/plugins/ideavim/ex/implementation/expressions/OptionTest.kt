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

package org.jetbrains.plugins.ideavim.ex.implementation.expressions

import com.maddyhome.idea.vim.option.OptionsManager.ignorecase
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class OptionTest : VimTestCase() {

  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test option`() {
    configureByText("\n")
    ignorecase.set()
    typeText(commandToKeys("if &ic | echo 'ignore case is on' | else | echo 'ignore case is off' | endif"))
    assertExOutput("ignore case is on\n")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test option2`() {
    configureByText("\n")
    ignorecase.reset()
    typeText(commandToKeys("if &ic | echo 'ignore case is on' | else | echo 'ignore case is off' | endif"))
    assertExOutput("ignore case is off\n")
  }
}
