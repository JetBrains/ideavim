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

class ExecuteCommandTest : VimTestCase() {

  @Test
  fun `test execute with one expression`() {
    configureByText("\n")
    typeText(commandToKeys("execute 'echo 42'"))
    assertOutput("42")
  }

  @Test
  fun `test execute with range`() {
    configureByText("\n")
    typeText(commandToKeys("1,2execute 'echo 42'"))
    assertNoOutput()
    assertPluginError(true)
  }

  @Test
  fun `test execute multiple expressions`() {
    configureByText("\n")
    typeText(commandToKeys("execute 'echo' 4 + 2 * 3"))
    assertOutput("10")
  }

  @Test
  fun `test execute adds space between expressions if missing`() {
    configureByText("\n")
    typeText(commandToKeys("execute 'echo ' . \"'result =\"4+2*3.\"'\""))
    assertOutput("result = 10")
  }

  @Test
  fun `test execute without spaces`() {
    configureByText("\n")
    typeText(commandToKeys("execute('echo '.42)"))
    assertOutput("42")
  }
}
