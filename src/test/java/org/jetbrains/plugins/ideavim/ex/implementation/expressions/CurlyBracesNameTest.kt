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

package org.jetbrains.plugins.ideavim.ex.implementation.expressions

import org.jetbrains.plugins.ideavim.VimTestCase

class CurlyBracesNameTest : VimTestCase() {

  fun `test name with expression`() {
    configureByText("\n")
    typeText(commandToKeys("let x1 = 10"))
    typeText(commandToKeys("echo x{0 + 1}"))
    assertExOutput("10\n")
  }

  fun `test name with inner template`() {
    configureByText("\n")
    typeText(commandToKeys("let eleven = 11"))
    typeText(commandToKeys("let z = 1"))
    typeText(commandToKeys("let y1 = 'leven'"))
    typeText(commandToKeys("echo e{y{z}}"))
    assertExOutput("11\n")
  }

  fun `test multiple templates inside a template`() {
    configureByText("\n")
    typeText(commandToKeys("let x1 = 'el'"))
    typeText(commandToKeys("let x2 = 'ev'"))
    typeText(commandToKeys("let x3 = 'en'"))
    typeText(commandToKeys("let eleven = 'twelve'"))
    typeText(commandToKeys("let twelve = 42"))

    typeText(commandToKeys("echo {x1}{x2}{x3}"))
    assertExOutput("twelve\n")

    typeText(commandToKeys("echo {{x1}{x2}{x3}}"))
    assertExOutput("42\n")
  }
}
