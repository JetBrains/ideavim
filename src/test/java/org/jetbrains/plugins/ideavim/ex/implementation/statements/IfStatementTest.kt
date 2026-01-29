/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.statements

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class IfStatementTest : VimTestCase() {

  @Test
  fun `test simple if with true condition`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        "" +
          "if 1 |" +
          " echo 'success' |" +
          "endif",
      ),
    )
    assertOutput("success")
  }

  @Test
  fun `test simple if with false condition`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        "" +
          "if 0 |" +
          " echo 'success' |" +
          "endif",
      ),
    )
    assertNoOutput()
  }

  @Test
  fun `test unreachable else`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        "" +
          "if 1 |" +
          " echo 'success' |" +
          "else |" +
          " echo 'failure' |" +
          "endif",
      ),
    )
    assertOutput("success")
  }

  @Test
  fun `test else`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        "" +
          "if 0 |" +
          " echo 'failure' |" +
          "else |" +
          " echo 'success' |" +
          "endif",
      ),
    )
    assertOutput("success")
  }

  @Test
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
          "endif",
      ),
    )
    assertOutput("success")
  }

  @Test
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
          "endif",
      ),
    )
    assertOutput("success")
  }

  @Test
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
          "endif",
      ),
    )
    assertOutput("success")
  }
}
