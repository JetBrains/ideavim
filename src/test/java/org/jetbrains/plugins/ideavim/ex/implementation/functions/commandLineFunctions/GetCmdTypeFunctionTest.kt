/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.functions.commandLineFunctions

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.ui.ex.ExEntryPanel
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class GetCmdTypeFunctionTest : VimTestCase() {

  @Test
  fun `test getcmdtype() for a regular command`() {
    configureByText("\n")
    enterCommand("cmap <expr> z getcmdtype()")
    typeText(":fooz")
    assertEquals("foo:", (injector.commandLine.getActiveCommandLine() as ExEntryPanel).visibleText)
  }

  @Test
  fun `test getcmdtype() for a forward search`() {
    configureByText("\n")
    enterCommand("cmap <expr> z getcmdtype()")
    typeText("/fooz")
    assertEquals("foo/", (injector.commandLine.getActiveCommandLine() as ExEntryPanel).visibleText)
  }

  @Test
  fun `test getcmdtype() for a backward search`() {
    configureByText("\n")
    enterCommand("cmap <expr> z getcmdtype()")
    typeText("?fooz")
    assertEquals("foo?", (injector.commandLine.getActiveCommandLine() as ExEntryPanel).visibleText)
  }

  @Test
  fun `test getcmdtype() for an expression command`() {
    configureByText("\n")
    enterCommand("cmap <expr> z getcmdtype()")
    typeText("i<C-r>=fooz")
    assertEquals("foo=", (injector.commandLine.getActiveCommandLine() as ExEntryPanel).visibleText)
  }

}
