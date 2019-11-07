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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package org.jetbrains.plugins.ideavim.ex

import com.maddyhome.idea.vim.ex.commands
import junit.framework.TestCase
import org.junit.Test
import kotlin.test.assertEquals

/**
 * @author Alex Plate
 */
class CommandParserTest {
  @Test
  fun `test one letter without optional`() {
    val commands = commands("a")
    assertEquals(1, commands.size)
    assertEquals("a", commands[0].required)
    assertEquals("", commands[0].optional)
  }

  @Test
  fun `test without optional`() {
    val commands = commands("a_discovery")
    TestCase.assertEquals(1, commands.size)
    assertEquals("a_discovery", commands[0].required)
    assertEquals("", commands[0].optional)
  }

  @Test
  fun `test with optional`() {
    val commands = commands("a[discovery]")
    TestCase.assertEquals(1, commands.size)
    assertEquals("a", commands[0].required)
    assertEquals("discovery", commands[0].optional)
  }
}
