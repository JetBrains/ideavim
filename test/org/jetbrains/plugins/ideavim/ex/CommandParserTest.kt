/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package org.jetbrains.plugins.ideavim.ex

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.ex.CommandParser
import com.maddyhome.idea.vim.ex.CommandParser.EX_COMMAND_EP
import com.maddyhome.idea.vim.ex.ExBeanClass
import com.maddyhome.idea.vim.ex.ExCommand
import com.maddyhome.idea.vim.ex.commands
import com.maddyhome.idea.vim.ex.ranges.Ranges
import junit.framework.TestCase
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

/**
 * @author Alex Plate
 */
class CommandParserTest : VimTestCase() {
  fun `test one letter without optional`() {
    val commands = commands("a")
    assertEquals(1, commands.size)
    assertEquals("a", commands[0].required)
    assertEquals("", commands[0].optional)
  }

  fun `test without optional`() {
    val commands = commands("a_discovery")
    TestCase.assertEquals(1, commands.size)
    assertEquals("a_discovery", commands[0].required)
    assertEquals("", commands[0].optional)
  }

  fun `test with optional`() {
    val commands = commands("a[discovery]")
    TestCase.assertEquals(1, commands.size)
    assertEquals("a", commands[0].required)
    assertEquals("discovery", commands[0].optional)
  }

  @TestWithoutNeovim(SkipNeovimReason.UNCLEAR, "Caret different position")
  fun `test simple ex command execution`() {
    val keys = ">>"
    val before = "I ${c}found it in a legendary land"
    val after = "    ${c}I found it in a legendary land"
    doTest(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun `test execute in disabled state`() {
    setupChecks {
      caretShape = false
    }
    val keys = commandToKeys(">>")
    val before = "I ${c}found it in a legendary land"
    val after = "I ${c}found it in a legendary land"
    doTestWithoutNeovim(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE) {
      VimPlugin.setEnabled(false)
    }
  }

  fun `test turn off and on`() {
    val keys = commandToKeys(">>")
    val before = "I ${c}found it in a legendary land"
    val after = "        ${c}I found it in a legendary land"
    doTestWithoutNeovim(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE) {
      VimPlugin.setEnabled(false)
      VimPlugin.setEnabled(true)
    }
  }

  fun `test turn off and on twice`() {
    val keys = commandToKeys(">>")
    val before = "I ${c}found it in a legendary land"
    val after = "        ${c}I found it in a legendary land"
    doTestWithoutNeovim(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE) {
      VimPlugin.setEnabled(false)
      VimPlugin.setEnabled(true)
      VimPlugin.setEnabled(true)
    }
  }

  fun `test unregister extension`() {
    val keys = commandToKeys(">>")
    val before = "I ${c}found it in a legendary land"
    val after = "        ${c}I found it in a legendary land"
    var extension: ExBeanClass? = null
    doTestWithoutNeovim(keys, before, after, CommandState.Mode.COMMAND, CommandState.SubMode.NONE) {
      extension = EX_COMMAND_EP.extensions().findFirst().get()

      // TODO: 08.02.2020 I'm sorry if your tests have been failed because of this code. Please update it properly
      TestCase.assertNotNull(CommandParser.getCommandHandler(ExCommand(Ranges(), "actionlist", "")))

      @Suppress("DEPRECATION")
      EX_COMMAND_EP.getPoint(null).unregisterExtension(extension!!)

      TestCase.assertNull(CommandParser.getCommandHandler(ExCommand(Ranges(), "actionlist", "")))
    }
    @Suppress("DEPRECATION")
    EX_COMMAND_EP.getPoint(null).registerExtension(extension!!)

    TestCase.assertNotNull(CommandParser.getCommandHandler(ExCommand(Ranges(), "actionlist", "")))
  }
}
