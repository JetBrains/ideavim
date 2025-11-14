/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class EchoCommandTest : VimTestCase("\n") {

  @Test
  fun `test echo with a string`() {
    assertCommandOutput("echo \"Hello, World!\"", "Hello, World!")
  }

  @Test
  fun `test echo with an expression`() {
    assertCommandOutput("echo 3 + 7", "10")
  }

  @Test
  fun `test echo with multiple expressions`() {
    assertCommandOutput("echo 3 + 7 'Hello ' . 'world'", "10 Hello world")
  }

  @Test
  fun `test ec`() {
    assertCommandOutput("ec 3", "3")
  }

  @Test
  fun `test echo without spaces`() {
    assertCommandOutput("echo(42)999", "42 999")
  }

  @Disabled("Vim evaluates the expression while parsing, and does not apply the (999) subscript to the first " +
    "expression because it knows it doesn't evaluate to a funcref/partial. " +
    "IdeaVim does not have this context, so I don't know how to fix this.")
  @Test
  fun `test echo without spaces 2`() {
    assertCommandOutput("echo(42)(999)", "42 999")
  }
}
