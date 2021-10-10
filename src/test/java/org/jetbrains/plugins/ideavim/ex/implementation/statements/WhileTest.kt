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

package org.jetbrains.plugins.ideavim.ex.implementation.statements

import org.jetbrains.plugins.ideavim.VimTestCase

class WhileTest : VimTestCase() {

  fun `test while`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        "" +
          "let x = 3 | " +
          "while x < 100 | " +
          " let x += 5 | " +
          "endwhile"
      )
    )
    typeText(commandToKeys("echo x"))
    assertExOutput("103\n")
  }

  fun `test while with break`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        "" +
          "let x = 3 |" +
          "while x < 100 | " +
          "  if x == 13 | " +
          "    break | " +
          "  else | " +
          "    let x += 5 | " +
          "  endif | " +
          "endwhile"
      )
    )
    typeText(commandToKeys("echo x"))
    assertExOutput("13\n")
  }

  fun `test while with continue`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        "" +
          "let evenNumbers = 0 |" +
          "let x = 0 |" +
          "while x < 100 | " +
          "  let x += 1 | " +
          "  if x % 2 == 1 | " +
          "    continue | " +
          "  endif |" +
          "  let evenNumbers += 1 | " +
          "endwhile"
      )
    )
    typeText(commandToKeys("echo evenNumbers"))
    assertExOutput("50\n")
  }
}
