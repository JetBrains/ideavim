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

import org.jetbrains.plugins.ideavim.VimTestCase

class BuiltInFunctionTest : VimTestCase() {

  fun `test abs`() {
    configureByText("\n")
    typeText(commandToKeys("echo abs(-123) abs(2)"))
    assertExOutput("123 2\n")
  }

  fun `test sin`() {
    configureByText("\n")
    typeText(commandToKeys("echo sin(0) sin(1)"))
    assertExOutput("0.0 0.841471\n")
  }

  fun `test empty`() {
    configureByText("\n")
    typeText(commandToKeys("echo empty(0) empty(1)"))
    assertExOutput("1 0\n")
    typeText(commandToKeys("echo empty(\"123\") empty(\"\")"))
    assertExOutput("0 1\n")
    typeText(commandToKeys("echo empty([1, 2]) empty([])"))
    assertExOutput("0 1\n")
    typeText(commandToKeys("echo empty({1:2}) empty({})"))
    assertExOutput("0 1\n")
  }
}
