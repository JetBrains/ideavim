/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.functions

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class BasicStringFunctions : VimTestCase() {

  @Test
  fun `test toupper`() {
    configureByText("\n")
    typeText(commandToKeys("echo toupper('Vim is awesome')"))
    assertExOutput("VIM IS AWESOME")
  }

  @Test
  fun `test tolower`() {
    configureByText("\n")
    typeText(commandToKeys("echo tolower('Vim is awesome')"))
    assertExOutput("vim is awesome")
  }

  @Test
  fun `test join`() {
    configureByText("\n")
    typeText(commandToKeys("echo join(['Vim', 'is', 'awesome'], '_')"))
    assertExOutput("Vim_is_awesome")
  }

  @Test
  fun `test join without second argument`() {
    configureByText("\n")
    typeText(commandToKeys("echo join(['Vim', 'is', 'awesome'])"))
    assertExOutput("Vim is awesome")
  }

  @Test
  fun `test join with wrong first argument type`() {
    configureByText("\n")
    typeText(commandToKeys("echo join('Vim is awesome')"))
    assertPluginError(true)
    assertPluginErrorMessageContains("E714: List required")
  }
}
