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

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.option.OptionsManager
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class LetCommandTest : VimTestCase() {

  fun `test assignment to string`() {
    configureByText("\n")
    typeText(commandToKeys("let s = \"foo\""))
    typeText(commandToKeys("echo s"))
    assertExOutput("foo\n")
  }

  fun `test assignment to number`() {
    configureByText("\n")
    typeText(commandToKeys("let s = 100"))
    typeText(commandToKeys("echo s"))
    assertExOutput("100\n")
  }

  fun `test assignment to expression`() {
    configureByText("\n")
    typeText(commandToKeys("let s = 10 + 20 * 4"))
    typeText(commandToKeys("echo s"))
    assertExOutput("90\n")
  }

  fun `test adding new pair to dictionary`() {
    configureByText("\n")
    typeText(commandToKeys("let s = {'key1' : 1}"))
    typeText(commandToKeys("let s['key2'] = 2"))
    typeText(commandToKeys("echo s"))
    assertExOutput("{'key1': 1, 'key2': 2}\n")
  }

  fun `test editing existing pair in dictionary`() {
    configureByText("\n")
    typeText(commandToKeys("let s = {'key1' : 1}"))
    typeText(commandToKeys("let s['key1'] = 2"))
    typeText(commandToKeys("echo s"))
    assertExOutput("{'key1': 2}\n")
  }

  fun `test assignment plus operator`() {
    configureByText("\n")
    typeText(commandToKeys("let s = 10"))
    typeText(commandToKeys("let s += 5"))
    typeText(commandToKeys("echo s"))
    assertExOutput("15\n")
  }

  fun `test changing list item`() {
    configureByText("\n")
    typeText(commandToKeys("let s = [1, 1]"))
    typeText(commandToKeys("let s[1] = 2"))
    typeText(commandToKeys("echo s"))
    assertExOutput("[1, 2]\n")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  fun `test changing list item with index out of range`() {
    configureByText("\n")
    typeText(commandToKeys("let s = [1, 1]"))
    typeText(commandToKeys("let s[2] = 2"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E684: list index out of range: 2")
  }

  fun `test changing list with sublist expression`() {
    configureByText("\n")
    typeText(commandToKeys("let s = [1, 2, 3]"))
    typeText(commandToKeys("let s[0:1] = [5, 4]"))
    typeText(commandToKeys("echo s"))
    assertExOutput("[5, 4, 3]\n")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  fun `test changing list with sublist expression and larger list`() {
    configureByText("\n")
    typeText(commandToKeys("let s = [1, 2, 3]"))
    typeText(commandToKeys("let s[0:1] = [5, 4, 3, 2, 1]"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E710: List value has more items than targets")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN_ERROR)
  fun `test changing list with sublist expression and smaller list`() {
    configureByText("\n")
    typeText(commandToKeys("let s = [1, 2, 3]"))
    typeText(commandToKeys("let s[0:1] = [5]"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E711: List value does not have enough items")
  }

  fun `test changing list with sublist expression and undefined end`() {
    configureByText("\n")
    typeText(commandToKeys("let s = [1, 2, 3]"))
    typeText(commandToKeys("let s[1:] = [5, 5, 5, 5]"))
    typeText(commandToKeys("echo s"))
    assertExOutput("[1, 5, 5, 5, 5]\n")
  }

  fun `test let option`() {
    configureByText("\n")
    typeText(commandToKeys("set noincsearch"))
    assertFalse(OptionsManager.incsearch.isSet)
    typeText(commandToKeys("let &incsearch = 12"))
    assertTrue(OptionsManager.incsearch.isSet)
    typeText(commandToKeys("set noincsearch"))
    assertFalse(OptionsManager.incsearch.isSet)
  }

  fun `test let option2`() {
    configureByText("\n")
    typeText(commandToKeys("set incsearch"))
    assertTrue(OptionsManager.incsearch.isSet)
    typeText(commandToKeys("let &incsearch = 0"))
    assertFalse(OptionsManager.incsearch.isSet)
  }

  fun `test comment`() {
    configureByText("\n")
    typeText(commandToKeys("let s = [1, 2, 3] \" my list for storing numbers"))
    typeText(commandToKeys("echo s"))
    assertExOutput("[1, 2, 3]\n")
  }
}
