/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class LockVarCommandTest : VimTestCase() {

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test lock int variable`() {
    configureByText("\n")
    typeText(commandToKeys("let x = 10"))
    typeText(commandToKeys("lockvar x"))
    typeText(commandToKeys("let x = 15"))
    assertPluginError(true)
    assertPluginErrorMessage("E741: Value is locked: x")
  }

  @Test
  fun `test unlock int variable`() {
    configureByText("\n")
    typeText(commandToKeys("let x = 10"))
    typeText(commandToKeys("lockvar x"))
    typeText(commandToKeys("unlockvar x"))
    typeText(commandToKeys("let x = 15"))
    assertPluginError(false)
    typeText(commandToKeys("echo x"))
    assertOutput("15")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test lock list variable`() {
    configureByText("\n")
    typeText(commandToKeys("let x = [1]"))
    typeText(commandToKeys("lockvar x"))
    typeText(commandToKeys("let x = 15"))
    assertPluginError(true)
    assertPluginErrorMessage("E741: Value is locked: x")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test lock list variable 2`() {
    configureByText("\n")
    typeText(commandToKeys("let x = [1]"))
    typeText(commandToKeys("lockvar x"))
    typeText(commandToKeys("let x += [2]"))
    assertPluginError(true)
    assertPluginErrorMessage("E741: Value is locked: x")
  }

  @Test
  fun `test reassigning assigned locked value`() {
    configureByText("\n")
    typeText(commandToKeys("let x = 10"))
    typeText(commandToKeys("lockvar x"))
    typeText(commandToKeys("let y = x"))
    typeText(commandToKeys("echo y"))
    assertOutput("10")
    typeText(commandToKeys("let y = 15"))
    typeText(commandToKeys("echo y"))
    assertOutput("15")
    assertPluginError(false)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test list elements are also locked`() {
    configureByText("\n")
    typeText(commandToKeys("let x = [1, 2]"))
    typeText(commandToKeys("lockvar x"))
    typeText(commandToKeys("let x[0] = 15"))
    assertPluginError(true)
    assertPluginErrorMessage("E741: Value is locked: x[0] = 15")
    typeText(commandToKeys("echo x"))
    assertOutput("[1, 2]")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test dict elements are also locked`() {
    configureByText("\n")
    typeText(commandToKeys("let x = {'one': 1}"))
    typeText(commandToKeys("lockvar x"))
    typeText(commandToKeys("let x.two = 2"))
    assertPluginError(true)
    assertPluginErrorMessage("E741: Value is locked: x.two = 2")
    typeText(commandToKeys("echo x"))
    assertOutput("{'one': 1}")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test can modify dict elements but not the dict itself`() {
    configureByText("\n")
    typeText(commandToKeys("let x = {'one': 1}"))
    typeText(commandToKeys("lockvar 1 x"))
    typeText(commandToKeys("let x.one = 42"))
    assertPluginError(false)
    typeText(commandToKeys("echo x"))
    assertOutput("{'one': 42}")
    typeText(commandToKeys("let x.two = 2"))
    assertPluginError(true)
    assertPluginErrorMessage("E741: Value is locked: x.two = 2")
    typeText(commandToKeys("echo x"))
    assertOutput("{'one': 42}")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test dict elements are also locked 2`() {
    configureByText("\n")
    typeText(commandToKeys("let x = {'one': 1}"))
    typeText(commandToKeys("lockvar x"))
    typeText(commandToKeys("let x['two'] = 2"))
    assertPluginError(true)
    assertPluginErrorMessage("E741: Value is locked: x['two'] = 2")
    typeText(commandToKeys("echo x"))
    assertOutput("{'one': 1}")
  }

  @Test
  fun `test default lock depth`() {
    configureByText("\n")
    typeText(commandToKeys("let x = {'list': [1]}"))
    typeText(commandToKeys("lockvar x"))
    typeText(commandToKeys("let x.list[0] = 42"))
    typeText(commandToKeys("echo x"))
    assertOutput("{'list': [42]}")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN_ERROR)
  @Test
  fun `test custom lock depth`() {
    configureByText("\n")
    typeText(commandToKeys("let x = {'list': [1]}"))
    typeText(commandToKeys("lockvar 3 x"))
    typeText(commandToKeys("let x.list[0] = 42"))
    assertPluginError(true)
    assertPluginErrorMessage("E741: Value is locked: x.list[0] = 42")
    typeText(commandToKeys("echo x"))
    assertOutput("{'list': [1]}")
  }
}
