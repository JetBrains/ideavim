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

class DictionaryElementByKeyTest : VimTestCase() {

  fun `test get element by key`() {
    configureByText("\n")
    typeText(commandToKeys("let dict = {'a': 42} | echo dict.a"))
    assertExOutput("42\n")
  }

  fun `test get element from inner dictionary`() {
    configureByText("\n")
    typeText(commandToKeys("let dict = {'a': 42, 'b' : {'c': 'oh, hi Mark'}} | echo dict.b.c"))
    assertExOutput("oh, hi Mark\n")
  }

  fun `test get element by key with minus`() {
    configureByText("\n")
    typeText(commandToKeys("let dict = {'first-key': 42, 'second-key' : {'third-key': 'oh, hi Mark'}} | echo dict.first-key"))
    assertExOutput("42\n")
  }

  fun `test get element from inner dictionary by keys with minuses`() {
    configureByText("\n")
    typeText(commandToKeys("let dict = {'first-key': 42, 'second-key' : {'third-key': 'oh, hi Mark'}} | echo dict.second-key.third-key"))
    assertExOutput("oh, hi Mark\n")
  }
}
