/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

/**
 * @author Elliot Courant
 */
class CmdCommandTest : VimTestCase() {
  @Test
  fun `test recursive`() {
    VimPlugin.getCommand().resetAliases()
    configureByText("\n")
    typeText(commandToKeys("command Recur1 Recur2"))
    assertPluginError(false)
    typeText(commandToKeys("command Recur2 Recur1"))
    assertPluginError(false)
    typeText(commandToKeys("Recur1"))
    assertPluginError(true) // Recursive command should error.
  }

  @Test
  fun `test list aliases`() {
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
    typeText(commandToKeys("command-nargs=* Test1 echo"))
    assertPluginError(false)
    typeText(commandToKeys("command-nargs=? Test2 echo"))
    assertPluginError(false)
    typeText(commandToKeys("command-nargs=+ Test3 echo"))
    assertPluginError(false)
    typeText(commandToKeys("command-nargs=1 Test4 echo"))
    assertPluginError(false)
    typeText(commandToKeys("command"))
    assertPluginError(false)
    // The added alias should be listed
    assertOutput(
      """Name        Args       Definition
            |Test1       *          echo
            |Test2       ?          echo
            |Test3       +          echo
            |Test4       1          echo
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
  }

  @Test
  fun `test bad alias`() {
    VimPlugin.getCommand().resetAliases()
    configureByText("\n")
    typeText(commandToKeys("Xyz"))
    assertPluginError(true)
    typeText(commandToKeys("command Xyz yank"))
    assertPluginError(false)
    typeText(commandToKeys("Xyz"))
    assertPluginError(false)
  }

  @Test
  fun `test lowercase should fail`() {
    VimPlugin.getCommand().resetAliases()
    configureByText("\n")
    typeText(commandToKeys("command lowercase vs"))
    assertPluginError(true)
  }

  @Test
  fun `test blacklisted alias should fail`() {
    VimPlugin.getCommand().resetAliases()
    configureByText("\n")
    typeText(commandToKeys("command X vs"))
    assertPluginError(true)
    typeText(commandToKeys("command Next vs"))
    assertPluginError(true)
    typeText(commandToKeys("command Print vs"))
    assertPluginError(true)
  }

  @Test
  fun `test add an existing alias and overwrite`() {
    VimPlugin.getCommand().resetAliases()
    configureByText("\n")
    typeText(commandToKeys("command Existing1 vs"))
    assertPluginError(false)
    typeText(commandToKeys("command Existing1 wq"))
    assertPluginError(true)
    typeText(commandToKeys("command! Existing1 wq"))
    assertPluginError(false)
  }

  @Test
  fun `test add command with arguments`() {
    VimPlugin.getCommand().resetAliases()
    configureByText("\n")
    typeText(commandToKeys("command -nargs=* Error echo <args>"))
    assertPluginError(false)
  }

  @Test
  fun `test add command with range`() {
    VimPlugin.getCommand().resetAliases()
    configureByText("\n")
    typeText(commandToKeys("command! -range Error echo <args>"))
    assertPluginError(false)
    kotlin.test.assertEquals("'-range' is not supported by `command`", injector.messages.getStatusBarMessage())
  }

  @Test
  fun `test add command with complete`() {
    VimPlugin.getCommand().resetAliases()
    configureByText("\n")
    typeText(commandToKeys("command! -complete=color Error echo <args>"))
    assertPluginError(false)
    kotlin.test.assertEquals("'-complete' is not supported by `command`", injector.messages.getStatusBarMessage())
  }

  @Test
  fun `test add command with arguments short`() {
    VimPlugin.getCommand().resetAliases()
    configureByText("\n")
    typeText(commandToKeys("command-nargs=* Error echo <args>"))
    assertPluginError(false)
  }

  @Test
  fun `test add command with arguments even shorter`() {
    VimPlugin.getCommand().resetAliases()
    configureByText("\n")
    typeText(commandToKeys("com-nargs=* Error echo <args>"))
    assertPluginError(false)
  }

  @Test
  fun `test add command with various arguments`() {
    VimPlugin.getCommand().resetAliases()
    configureByText("\n")
    typeText(commandToKeys("command! -nargs=0 Error echo <args>"))
    assertPluginError(false)
    typeText(commandToKeys("command! -nargs=1 Error echo <args>"))
    assertPluginError(false)
    typeText(commandToKeys("command! -nargs=* Error echo <args>"))
    assertPluginError(false)
    typeText(commandToKeys("command! -nargs=? Error echo <args>"))
    assertPluginError(false)
    typeText(commandToKeys("command! -nargs=+ Error echo <args>"))
    assertPluginError(false)
  }

  @Test
  fun `test add command with invalid arguments`() {
    VimPlugin.getCommand().resetAliases()
    configureByText("\n")
    typeText(commandToKeys("command! -nargs= Error echo <args>"))
    assertPluginError(true)
    typeText(commandToKeys("command! -nargs=-1 Error echo <args>"))
    assertPluginError(true)
    typeText(commandToKeys("command! -nargs=# Error echo <args>"))
    assertPluginError(true)
  }

  @Test
  fun `test run command with arguments`() {
    VimPlugin.getCommand().resetAliases()
    configureByText("\n")
    typeText(commandToKeys("let test = \"Hello!\""))
    assertPluginError(false)
    typeText(commandToKeys("command! -nargs=1 Error echo <args>"))
    assertPluginError(false)
    typeText(commandToKeys("Error test"))
    assertPluginError(false)
    assertOutput("Hello!")

    typeText(commandToKeys("command! -nargs=1 Error echo <q-args>"))
    assertPluginError(false)
    typeText(commandToKeys("Error test message"))
    assertPluginError(false)
    assertOutput("test message")
  }

  @Test
  fun `test run command that creates another command`() {
    VimPlugin.getCommand().resetAliases()
    configureByText("\n")
    typeText(commandToKeys("command! -nargs=1 CreateCommand command -nargs=1 <args> <lt>q-args>"))
    assertPluginError(false)
    typeText(commandToKeys("CreateCommand Show echo"))
    assertPluginError(false)
    typeText(commandToKeys("Show test"))
    assertPluginError(false)
    assertOutput("test")
  }

  @Test
  fun `test run command missing required argument`() {
    VimPlugin.getCommand().resetAliases()
    configureByText("\n")
    typeText(commandToKeys("command! -nargs=1 Error echo <q-args>"))
    assertPluginError(false)
    typeText(commandToKeys("Error"))
  }
}
