/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.functions.listFunctions

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class SplitFunctionTest : VimTestCase() {
  @Test
  fun `test split with default delimiter`() {
    configureByText("\n")
    typeText(commandToKeys("echo split('Hello world')"))
    assertExOutput("['Hello', 'world']")
  }

  @Test
  fun `test split comma separated text`() {
    configureByText("\n")
    typeText(commandToKeys("echo split('a,b,c,d', ',')"))
    assertExOutput("['a', 'b', 'c', 'd']")
  }

  @Test
  fun `test split comma separated text 2`() {
    configureByText("\n")
    typeText(commandToKeys("echo split(',a,b,c,d,', ',')"))
    assertExOutput("['a', 'b', 'c', 'd']")
  }

  @Test
  fun `test split comma separated text with keepempty flag`() {
    configureByText("\n")
    typeText(commandToKeys("echo split(',a,b,c,,d,', ',', 1)"))
    assertExOutput("['', 'a', 'b', 'c', '', 'd', '']")
  }
}
