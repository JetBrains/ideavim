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

class WhileTest : VimTestCase() {

  @Test
  fun `test while`() {
    configureByText("\n")
    typeText(
      commandToKeys(
        "" +
          "let x = 3 | " +
          "while x < 100 | " +
          " let x += 5 | " +
          "endwhile",
      ),
    )
    typeText(commandToKeys("echo x"))
    assertOutput("103")
  }

  @Test
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
          "endwhile",
      ),
    )
    typeText(commandToKeys("echo x"))
    assertOutput("13")
  }

  @Test
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
          "endwhile",
      ),
    )
    typeText(commandToKeys("echo evenNumbers"))
    assertOutput("50")
  }
}
