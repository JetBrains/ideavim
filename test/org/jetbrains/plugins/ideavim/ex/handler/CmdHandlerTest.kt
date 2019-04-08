/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.jetbrains.plugins.ideavim.ex.handler

import com.maddyhome.idea.vim.VimPlugin
import org.jetbrains.plugins.ideavim.VimFileEditorTestCase
import org.jetbrains.plugins.ideavim.VimTestCase

/**
 * @author Elliot Courant
 */
class CmdHandlerTest : VimFileEditorTestCase() {
    fun `test recursive`() {
        VimPlugin.getCommand().resetAliases()
        configureByText("\n")
        typeText(VimTestCase.commandToKeys("command Recur1 Recur2"))
        assertPluginError(false)
        typeText(VimTestCase.commandToKeys("command Recur2 Recur1"))
        assertPluginError(false)
        typeText(VimTestCase.commandToKeys("Recur1"))
        assertPluginError(true) // Recursive command should error.
    }

    fun `test list aliases`() {
        VimPlugin.getCommand().resetAliases()
        configureByText("\n")
        typeText(VimTestCase.commandToKeys("command"))
        assertPluginError(false)
        assertExOutput("Name        Args       Definition\n") // There should not be any aliases.

        typeText(VimTestCase.commandToKeys("command Vs vs"))
        assertPluginError(false)
        typeText(VimTestCase.commandToKeys("command Wq wq"))
        assertPluginError(false)
        typeText(VimTestCase.commandToKeys("command WQ wq"))
        assertPluginError(false)
        typeText(VimTestCase.commandToKeys("command-nargs=* Test1 echo"))
        assertPluginError(false)
        typeText(VimTestCase.commandToKeys("command-nargs=? Test2 echo"))
        assertPluginError(false)
        typeText(VimTestCase.commandToKeys("command-nargs=+ Test3 echo"))
        assertPluginError(false)
        typeText(VimTestCase.commandToKeys("command-nargs=1 Test4 echo"))
        assertPluginError(false)
        typeText(VimTestCase.commandToKeys("command"))
        assertPluginError(false)
        // The added alias should be listed
        assertExOutput("""Name        Args       Definition
            |Test1       *          echo
            |Test2       ?          echo
            |Test3       +          echo
            |Test4       1          echo
            |Vs          0          vs
            |Wq          0          wq
            |WQ          0          wq
        """.trimMargin())

        typeText(VimTestCase.commandToKeys("command W"))
        assertPluginError(false)
        // The filtered aliases should be listed
        assertExOutput("""Name        Args       Definition
            |Wq          0          wq
            |WQ          0          wq
        """.trimMargin())
    }

    fun `test bad alias`() {
        VimPlugin.getCommand().resetAliases()
        configureByText("\n")
        typeText(VimTestCase.commandToKeys("Vs"))
        assertPluginError(true)
        typeText(VimTestCase.commandToKeys("command Vs vs"))
        assertPluginError(false)
        typeText(VimTestCase.commandToKeys("Vs"))
        assertPluginError(false)
    }

    fun `test lowercase should fail`() {
        VimPlugin.getCommand().resetAliases()
        configureByText("\n")
        typeText(VimTestCase.commandToKeys("command lowercase vs"))
        assertPluginError(true)
    }

    fun `test blacklisted alias should fail`() {
        VimPlugin.getCommand().resetAliases()
        configureByText("\n")
        typeText(VimTestCase.commandToKeys("command X vs"))
        assertPluginError(true)
        typeText(VimTestCase.commandToKeys("command Next vs"))
        assertPluginError(true)
        typeText(VimTestCase.commandToKeys("command Print vs"))
        assertPluginError(true)
    }

    fun `test add an existing alias and overwrite`() {
        VimPlugin.getCommand().resetAliases()
        configureByText("\n")
        typeText(VimTestCase.commandToKeys("command Existing1 vs"))
        assertPluginError(false)
        typeText(VimTestCase.commandToKeys("command Existing1 wq"))
        assertPluginError(true)
        typeText(VimTestCase.commandToKeys("command! Existing1 wq"))
        assertPluginError(false)
    }

    fun `test add command with arguments`() {
        VimPlugin.getCommand().resetAliases()
        configureByText("\n")
        typeText(VimTestCase.commandToKeys("command -nargs=* Error echo <args>"))
        assertPluginError(false)
    }

    fun `test add command with arguments short`() {
        VimPlugin.getCommand().resetAliases()
        configureByText("\n")
        typeText(VimTestCase.commandToKeys("command-nargs=* Error echo <args>"))
        assertPluginError(false)
    }

    fun `test add command with arguments even shorter`() {
        VimPlugin.getCommand().resetAliases()
        configureByText("\n")
        typeText(VimTestCase.commandToKeys("com-nargs=* Error echo <args>"))
        assertPluginError(false)
    }

    fun `test add command with various arguments`() {
        VimPlugin.getCommand().resetAliases()
        configureByText("\n")
        typeText(VimTestCase.commandToKeys("command! -nargs=0 Error echo <args>"))
        assertPluginError(false)
        typeText(VimTestCase.commandToKeys("command! -nargs=1 Error echo <args>"))
        assertPluginError(false)
        typeText(VimTestCase.commandToKeys("command! -nargs=* Error echo <args>"))
        assertPluginError(false)
        typeText(VimTestCase.commandToKeys("command! -nargs=? Error echo <args>"))
        assertPluginError(false)
        typeText(VimTestCase.commandToKeys("command! -nargs=+ Error echo <args>"))
        assertPluginError(false)
    }

    fun `test add command with invalid arguments`() {
        VimPlugin.getCommand().resetAliases()
        configureByText("\n")
        typeText(VimTestCase.commandToKeys("command! -nargs= Error echo <args>"))
        assertPluginError(true)
        typeText(VimTestCase.commandToKeys("command! -nargs=-1 Error echo <args>"))
        assertPluginError(true)
        typeText(VimTestCase.commandToKeys("command! -nargs=# Error echo <args>"))
        assertPluginError(true)
    }

    fun `test run command with arguments`() {
        VimPlugin.getCommand().resetAliases()
        configureByText("\n")
        typeText(VimTestCase.commandToKeys("let test = \"Hello!\""))
        assertPluginError(false)
        typeText(VimTestCase.commandToKeys("command! -nargs=1 Error echo <args>"))
        assertPluginError(false)
        typeText(VimTestCase.commandToKeys("Error test"))
        assertPluginError(false)
        assertExOutput("Hello!\n")

        typeText(VimTestCase.commandToKeys("command! -nargs=1 Error echo <q-args>"))
        assertPluginError(false)
        typeText(VimTestCase.commandToKeys("Error test message"))
        assertPluginError(false)
        assertExOutput("test message\n")
    }

    fun `test run command with f-args`() {
        VimPlugin.getCommand().resetAliases()
        configureByText("\n")
        typeText(VimTestCase.commandToKeys("command! -nargs=1 Error echo <f-args>"))
        assertPluginError(false)
        typeText(VimTestCase.commandToKeys("Error test message"))
        assertPluginError(false) // because it will combine test and message into a single arg
        assertExOutput("test message\n")
    }

    fun `test run command with bad arguments`() {
        VimPlugin.getCommand().resetAliases()
        configureByText("\n")
        typeText(VimTestCase.commandToKeys("command! -nargs=* Error echo <f-args>"))
        assertPluginError(false)
        typeText(VimTestCase.commandToKeys("Error test test"))
        assertPluginError(true) // should fail because you cant echo multiple items
    }

    fun `test run command that creates another command`() {
        VimPlugin.getCommand().resetAliases()
        configureByText("\n")
        typeText(VimTestCase.commandToKeys("command! -nargs=1 CreateCommand command -nargs=1 <args> <lt>q-args>"))
        assertPluginError(false)
        typeText(VimTestCase.commandToKeys("CreateCommand Show echo"))
        assertPluginError(false)
        typeText(VimTestCase.commandToKeys("Show test"))
        assertPluginError(false)
        assertExOutput("test\n")
    }

    fun `test run command missing required argument`() {
        VimPlugin.getCommand().resetAliases()
        configureByText("\n")
        typeText(VimTestCase.commandToKeys("command! -nargs=1 Error echo <q-args>"))
        assertPluginError(false)
        typeText(VimTestCase.commandToKeys("Error"))
    }

    fun `test run command calling function with multiple args`() {
        VimPlugin.getCommand().resetAliases()
        configureByText("\n")
        typeText(VimTestCase.commandToKeys("command! -nargs=* Error echo \"<f-args>\""))
        assertPluginError(false)

        typeText(VimTestCase.commandToKeys("Error one two"))
        assertPluginError(false)
        assertExOutput("'one', 'two'\n")

        typeText(VimTestCase.commandToKeys("Error one\\ two"))
        assertPluginError(false)
        assertExOutput("'one two'\n")
    }
}