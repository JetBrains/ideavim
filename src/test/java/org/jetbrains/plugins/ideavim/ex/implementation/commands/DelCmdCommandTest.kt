/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.VimPlugin
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

/**
 * @author Elliot Courant
 */
class DelCmdCommandTest : VimTestCase() {
  @Test
  fun `test remove alias`() {
    VimPlugin.getCommand().resetAliases()
    configureByText("\n")
    typeText(commandToKeys("command"))
    assertPluginError(false)
    assertOutput("Name        Args       Definition") // There should not be any aliases.

    typeText(commandToKeys("command Vs vs"))
    assertPluginError(false)
    typeText(commandToKeys("command Wq wq"))
    assertPluginError(false)
    typeText(commandToKeys("command WQ wq"))
    assertPluginError(false)
    typeText(commandToKeys("command"))
    assertPluginError(false)
    // The added alias should be listed
    assertOutput(
      """Name        Args       Definition
            |Vs          0          vs
            |Wq          0          wq
            |WQ          0          wq
      """.trimMargin(),
    )

    typeText(commandToKeys("command W"))
    assertPluginError(false)
    // The filtered aliases should be listed
    assertOutput(
      """Name        Args       Definition
            |Wq          0          wq
            |WQ          0          wq
      """.trimMargin(),
    )

    // Delete one of the aliases and then list all aliases again.
    typeText(commandToKeys("delcommand Wq"))
    assertPluginError(false)
    typeText(commandToKeys("command"))
    assertPluginError(false)
    assertOutput(
      """Name        Args       Definition
            |Vs          0          vs
            |WQ          0          wq
      """.trimMargin(),
    )
  }

  @Test
  fun `test remove non-existant alias`() {
    VimPlugin.getCommand().resetAliases()
    configureByText("\n")
    typeText(commandToKeys("delcommand VS"))
    assertPluginError(true)
  }
}
