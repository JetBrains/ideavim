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
package org.jetbrains.plugins.ideavim.ex.handler

import com.maddyhome.idea.vim.helper.StringHelper
import org.jetbrains.plugins.ideavim.VimTestCase

/**
 * @author John Weigel
 */
class BufferHandlerTest : VimTestCase() {
  fun testBufferActionByNumber() {
    configureByFileName("aaa.txt")
    configureByFileName("bbb.txt")
    typeText(commandToKeys("buffer 2"))

    assertPluginError(false)
  }

  fun testBufferActionByName() {
    configureByFileName("aaa.txt")
    configureByFileName("bbb.txt")
    typeText(commandToKeys("buffer aaa"))

    assertPluginError(false)
  }

  fun testBufferActionWithNoArg() {
    configureByText("\n")
    typeText(commandToKeys("buffer"))

    assertPluginError(false)
  }

  fun testBufferActionWithInvalidBufferNumber() {
    configureByText("\n")
    typeText(commandToKeys("buffer 999"))

    assertPluginError(true)
  }

  fun testBufferActionWithInvalidBufferName() {
    configureByText("\n")
    typeText(commandToKeys("buffer invalidbuffer"))

    assertPluginError(true)
  }

  fun testBufferActionWithModifications() {
    configureByFileName("aaa.txt")
    configureByFileName("bbb.txt")
    typeText(StringHelper.parseKeys("aa<esc>:buffer aaa<enter>"))

    assertPluginError(true)
  }

  fun testBufferActionWithModificationsOverride() {
    configureByFileName("aaa.txt")
    configureByFileName("bbb.txt")
    typeText(StringHelper.parseKeys("aa<esc>:buffer! aaa<enter>"))

    assertPluginError(false)
  }

  fun testBufferActionWithMultipleMatches() {
    configureByFileName("aaa.txt")
    configureByFileName("aaa2.txt")
    typeText(commandToKeys("buffer aaa"))

    assertPluginError(true)
  }

  fun testBufAction() {
    configureByText("\n")
    typeText(commandToKeys("buf 1"))

    assertPluginError(false)
  }

  fun testBuAction() {
    configureByText("\n")
    typeText(commandToKeys("bu 1"))

    assertPluginError(false)
  }

  fun testBAction() {
    configureByText("\n")
    typeText(commandToKeys("b 1"))

    assertPluginError(false)
  }
}
