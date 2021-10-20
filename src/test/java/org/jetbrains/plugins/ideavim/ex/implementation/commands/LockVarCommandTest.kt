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

import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class LockVarCommandTest : VimTestCase() {

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  fun `test lock int variable`() {
    configureByText("\n")
    typeText(commandToKeys("let x = 10"))
    typeText(commandToKeys("lockvar x"))
    typeText(commandToKeys("let x = 15"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E741: Value is locked: x")
  }

  fun `test unlock int variable`() {
    configureByText("\n")
    typeText(commandToKeys("let x = 10"))
    typeText(commandToKeys("lockvar x"))
    typeText(commandToKeys("unlockvar x"))
    typeText(commandToKeys("let x = 15"))
    assertPluginError(false)
    typeText(commandToKeys("echo x"))
    assertExOutput("15\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  fun `test lock list variable`() {
    configureByText("\n")
    typeText(commandToKeys("let x = [1]"))
    typeText(commandToKeys("lockvar x"))
    typeText(commandToKeys("let x = 15"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E741: Value is locked: x")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  fun `test lock list variable 2`() {
    configureByText("\n")
    typeText(commandToKeys("let x = [1]"))
    typeText(commandToKeys("lockvar x"))
    typeText(commandToKeys("let x += [2]"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E741: Value is locked: x")
  }

  fun `test reassigning assigned locked value`() {
    configureByText("\n")
    typeText(commandToKeys("let x = 10"))
    typeText(commandToKeys("lockvar x"))
    typeText(commandToKeys("let y = x"))
    typeText(commandToKeys("echo y"))
    assertExOutput("10\n")
    typeText(commandToKeys("let y = 15"))
    typeText(commandToKeys("echo y"))
    assertExOutput("15\n")
    assertPluginError(false)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  fun `test list elements are also locked`() {
    configureByText("\n")
    typeText(commandToKeys("let x = [1, 2]"))
    typeText(commandToKeys("lockvar x"))
    typeText(commandToKeys("let x[0] = 15"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E741: Value is locked")
    typeText(commandToKeys("echo x"))
    assertExOutput("[1, 2]\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  fun `test dict elements are also locked`() {
    configureByText("\n")
    typeText(commandToKeys("let x = {'one': 1}"))
    typeText(commandToKeys("lockvar x"))
    typeText(commandToKeys("let x.two = 2"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E741: Value is locked")
    typeText(commandToKeys("echo x"))
    assertExOutput("{'one': 1}\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  fun `test can modify dict elements but not the dict itself`() {
    configureByText("\n")
    typeText(commandToKeys("let x = {'one': 1}"))
    typeText(commandToKeys("lockvar 1 x"))
    typeText(commandToKeys("let x.one = 42"))
    assertPluginError(false)
    typeText(commandToKeys("echo x"))
    assertExOutput("{'one': 42}\n")
    typeText(commandToKeys("let x.two = 2"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E741: Value is locked")
    typeText(commandToKeys("echo x"))
    assertExOutput("{'one': 42}\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  fun `test dict elements are also locked 2`() {
    configureByText("\n")
    typeText(commandToKeys("let x = {'one': 1}"))
    typeText(commandToKeys("lockvar x"))
    typeText(commandToKeys("let x['two'] = 2"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E741: Value is locked")
    typeText(commandToKeys("echo x"))
    assertExOutput("{'one': 1}\n")
  }

  fun `test default lock depth`() {
    configureByText("\n")
    typeText(commandToKeys("let x = {'list': [1]}"))
    typeText(commandToKeys("lockvar x"))
    typeText(commandToKeys("let x.list[0] = 42"))
    typeText(commandToKeys("echo x"))
    assertExOutput("{'list': [42]}\n")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  fun `test custom lock depth`() {
    configureByText("\n")
    typeText(commandToKeys("let x = {'list': [1]}"))
    typeText(commandToKeys("lockvar 3 x"))
    typeText(commandToKeys("let x.list[0] = 42"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E741: Value is locked")
    typeText(commandToKeys("echo x"))
    assertExOutput("{'list': [1]}\n")
  }
}
