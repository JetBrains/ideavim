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

class IfStatementTest : VimTestCase() {

  fun `test simple if with true condition`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        "" +
          "if 1 |" +
          " echo 'success' |" +
          "endif"
      )
    )
    assertExOutput("success\n")
  }

  fun `test simple if with false condition`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        "" +
          "if 0 |" +
          " echo 'success' |" +
          "endif"
      )
    )
    assertNoExOutput()
  }

  fun `test unreachable else`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        "" +
          "if 1 |" +
          " echo 'success' |" +
          "else |" +
          " echo 'failure' |" +
          "endif"
      )
    )
    assertExOutput("success\n")
  }

  fun `test else`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        "" +
          "if 0 |" +
          " echo 'failure' |" +
          "else |" +
          " echo 'success' |" +
          "endif"
      )
    )
    assertExOutput("success\n")
  }

  fun `test unreachable elif`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        "" +
          "if 1 |" +
          " echo 'success' |" +
          "elseif 1 |" +
          " echo 'failure' |" +
          "else |" +
          " echo 'failure' |" +
          "endif"
      )
    )
    assertExOutput("success\n")
  }

  fun `test elif`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        "" +
          "if 0 |" +
          " echo 'failure' |" +
          "elseif 1 |" +
          " echo 'success' |" +
          "else |" +
          " echo 'failure' |" +
          "endif"
      )
    )
    assertExOutput("success\n")
  }

  fun `test multiple elifs`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        "" +
          "if 0 |" +
          " echo 'failure' |" +
          "elseif 0 |" +
          " echo 'failure' |" +
          "elseif 1 |" +
          " echo 'success' |" +
          "else |" +
          " echo 'failure' |" +
          "endif"
      )
    )
    assertExOutput("success\n")
  }
}
