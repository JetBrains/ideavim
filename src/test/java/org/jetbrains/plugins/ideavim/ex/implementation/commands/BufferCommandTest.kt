/*
 * Copyright 2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.api.injector
import org.jetbrains.plugins.ideavim.VimTestCase

/**
 * @author John Weigel
 */
class BufferCommandTest : VimTestCase() {
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
    typeText(injector.parser.parseKeys("aa<esc>:buffer aaa<enter>"))

    assertPluginError(true)
  }

  fun testBufferActionWithModificationsOverride() {
    configureByFileName("aaa.txt")
    configureByFileName("bbb.txt")
    typeText(injector.parser.parseKeys("aa<esc>:buffer! aaa<enter>"))

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
