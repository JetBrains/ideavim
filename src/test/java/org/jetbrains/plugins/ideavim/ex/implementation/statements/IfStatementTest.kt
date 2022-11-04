/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
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
