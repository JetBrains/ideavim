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

class SublistExpressionTest : VimTestCase() {

  fun `test strung sublist`() {
    configureByText("\n")
    typeText(commandToKeys("echo 'abc'[0:1]"))
    assertExOutput("ab\n")
  }

  fun `test negative index with sting`() {
    configureByText("\n")
    typeText(commandToKeys("echo 'abc'[-1]"))
    assertExOutput("\n")
  }

  fun `test index greater than size with string`() {
    configureByText("\n")
    typeText(commandToKeys("echo 'abc'[1000]"))
    assertExOutput("\n")
  }

  fun `test negative index with list`() {
    configureByText("\n")
    typeText(commandToKeys("echo [1, 2][-1]"))
    assertPluginErrorMessageContains("E684: list index out of range: -1")
  }

  fun `test index greater than size with list`() {
    configureByText("\n")
    typeText(commandToKeys("echo [1, 2][1000]"))
    assertPluginErrorMessageContains("E684: list index out of range: 1000")
  }

  fun `test negative first index`() {
    configureByText("\n")
    typeText(commandToKeys("echo 'abc'[-1:]"))
    assertExOutput("c\n")
  }

  fun `test negative last index`() {
    configureByText("\n")
    typeText(commandToKeys("echo 'abc'[0:-2]"))
    assertExOutput("ab\n")
  }

  fun `test negative last index2`() {
    configureByText("\n")
    typeText(commandToKeys("echo 'abc'[0:-1]"))
    assertExOutput("abc\n")
  }

  fun `test last index bigger sting size`() {
    configureByText("\n")
    typeText(commandToKeys("echo 'abc'[1:10000]"))
    assertExOutput("bc\n")
  }

  fun `test both indexes bigger sting size`() {
    configureByText("\n")
    typeText(commandToKeys("echo 'abc'[100:10000]"))
    assertExOutput("\n")
  }

  fun `test first index is bigger than second`() {
    configureByText("\n")
    typeText(commandToKeys("echo 'abc'[100:10]"))
    assertExOutput("\n")
  }
}
