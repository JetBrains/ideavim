/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class EchoCommandTest : VimTestCase() {

  @Test
  fun `test echo with a string`() {
    configureByText("\n")
    typeText(commandToKeys("echo \"Hello, World!\""))
    assertExOutput("Hello, World!")
  }

  @Test
  fun `test echo with an expression`() {
    configureByText("\n")
    typeText(commandToKeys("echo 3 + 7"))
    assertExOutput("10")
  }

  @Test
  fun `test echo with multiple expressions`() {
    configureByText("\n")
    typeText(commandToKeys("echo 3 + 7 'Hello ' . 'world'"))
    assertExOutput("10 Hello world")
  }

  @Test
  fun `test ec`() {
    configureByText("\n")
    typeText(commandToKeys("ec 3"))
    assertExOutput("3")
  }

  @Test
  fun `test echo without spaces`() {
    configureByText("\n")
    typeText(commandToKeys("echo(42)(999)"))
    assertExOutput("42 999")
  }
}
