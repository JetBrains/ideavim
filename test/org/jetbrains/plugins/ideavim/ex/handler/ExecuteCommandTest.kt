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

package org.jetbrains.plugins.ideavim.ex.handler

import org.jetbrains.plugins.ideavim.VimTestCase

class ExecuteCommandTest : VimTestCase() {

  fun `test execute with one expression`() {
    configureByText("\n")
    typeText(commandToKeys("execute 'echo 42'"))
    assertExOutput("42\n")
  }

  fun `test execute with range`() {
    configureByText("\n")
    typeText(commandToKeys("1,2execute 'echo 42'"))
    assertNoExOutput()
    assertPluginError(true)
  }

  fun `test execute multiple expressions`() {
    configureByText("\n")
    typeText(commandToKeys("execute 'echo' 4 + 2 * 3"))
    assertExOutput("10\n")
  }

  fun `test execute adds space between expressions if missing`() {
    configureByText("\n")
    typeText(commandToKeys("execute 'echo ' . \"'result =\"4+2*3.\"'\""))
    assertExOutput("result = 10\n")
  }
}
