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

import org.jetbrains.plugins.ideavim.VimTestCase

class EchoCommandTest : VimTestCase() {

  fun `test echo with a string`() {
    configureByText("\n")
    typeText(commandToKeys("echo \"Hello, World!\""))
    assertExOutput("Hello, World!\n")
  }

  fun `test echo with an expression`() {
    configureByText("\n")
    typeText(commandToKeys("echo 3 + 7"))
    assertExOutput("10\n")
  }

  fun `test echo with multiple expressions`() {
    configureByText("\n")
    typeText(commandToKeys("echo 3 + 7 'Hello ' . 'world'"))
    assertExOutput("10 Hello world\n")
  }

  fun `test ec`() {
    configureByText("\n")
    typeText(commandToKeys("ec 3"))
    assertExOutput("3\n")
  }
}
