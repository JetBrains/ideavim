/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.option

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class MessagesOptOptionTest : VimTestCase("\n") {
  @Test
  fun `test messagesopt must contain history`() {
    enterCommand("set messagesopt=history:100,hit-enter")
    assertPluginError(false)
  }

  @Test
  fun `test wait must be a number`() {
    enterCommand("set messagesopt=history:100,wait:foo")
    assertPluginError(true)
    assertPluginErrorMessage("E474: Invalid argument: messagesopt=history:100,wait:foo")
  }

  @Test
  fun `test wait must be greater than zero`() {
    enterCommand("set messagesopt=history:100,wait:-3")
    assertPluginError(true)
    assertPluginErrorMessage("E474: Invalid argument: messagesopt=history:100,wait:-3")
  }

  @Test
  fun `test wait must be less than 10000`() {
    enterCommand("set messagesopt=history:100,wait:10001")
    assertPluginError(true)
    assertPluginErrorMessage("E474: Invalid argument: messagesopt=history:100,wait:10001")
  }

  @Test
  fun `test wait must be between 0 and 10000 inclusive`() {
    enterCommand("set messagesopt=history:100,wait:0")
    assertPluginError(false)
    enterCommand("set messagesopt=history:100,wait:10000")
    assertPluginError(false)
  }

  @Test
  fun `test messagesopt must contain either hit-enter or wait`() {
    enterCommand("set messagesopt=history:100")
    assertPluginError(true)
    // There is no specific error message for this case
    assertPluginErrorMessage("E474: Invalid argument: messagesopt=history:100")
  }

  @Test
  fun `test messagesopt must contain hit-enter if wait is missing`() {
    enterCommand("set messagesopt=hit-enter,history:100")
    assertPluginError(false)
  }

  @Test
  fun `test messagesopt must contain wait if hit-enter is missing`() {
    enterCommand("set messagesopt=history:100")
    assertPluginError(true)
    assertPluginErrorMessage("E474: Invalid argument: messagesopt=history:100")
  }

  @Test
  fun `test history must be a number`() {
    enterCommand("set messagesopt=hit-enter,history:foo")
    assertPluginError(true)
    assertPluginErrorMessage("E474: Invalid argument: messagesopt=hit-enter,history:foo")
  }

  @Test
  fun `test history must be greater than zero`() {
    enterCommand("set messagesopt=hit-enter,history:-1")
    assertPluginError(true)
    assertPluginErrorMessage("E474: Invalid argument: messagesopt=hit-enter,history:-1")
  }

  @Test
  fun `test messagesopt cannot remove wait if hit-enter is not present`() {
    enterCommand("set messagesopt=history:100,wait:10000")
    assertPluginError(false)
    enterCommand("set mopt-=wait:10000")
    assertPluginError(true)
    assertPluginErrorMessage("E474: Invalid argument: mopt-=wait:10000")
  }

  @Test
  fun `test messagesopt cannot remove hit-enter if wait is not present`() {
    enterCommand("set messagesopt=history:100,hit-enter")
    assertPluginError(false)
    enterCommand("set mopt-=hit-enter")
    assertPluginError(true)
    assertPluginErrorMessage("E474: Invalid argument: mopt-=hit-enter")
  }

  @Test
  fun `test messagesopt cannot remove history`() {
    enterCommand("set messagesopt=history:100,hit-enter")
    assertPluginError(false)
    enterCommand("set mopt-=history:100")
    assertPluginError(true)
    assertPluginErrorMessage("E474: Invalid argument: mopt-=history:100")
  }
}
