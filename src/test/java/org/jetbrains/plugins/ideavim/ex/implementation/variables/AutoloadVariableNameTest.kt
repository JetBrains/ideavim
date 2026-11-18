/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.variables

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import kotlin.test.assertEquals

class AutoloadVariableNameTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `namespaced global variable is readable by its full name`() {
    executeVimscript("let g:foo#bar#baz = 'HELLO'")
    assertCommandOutput("echo g:foo#bar#baz", "HELLO")
  }

  @Test
  fun `namespaced variable without scope is readable by its full name`() {
    executeVimscript("let foo#bar = 'HELLO'")
    assertCommandOutput("echo foo#bar", "HELLO")
  }

  @Test
  fun `namespaced global variable is stored under its full name`() {
    executeVimscript("let g:foo#bar#baz = 'HELLO'")
    assertEquals(VimString("HELLO"), injector.variableService.getGlobalVariableValue("foo#bar#baz"))
  }

  @Test
  fun `namespaced global variable is not resolvable by its last segment`() {
    executeVimscript("let g:foo#bar#baz = 'HELLO'")
    enterCommand("echo g:baz")
    assertPluginErrorMessage("E121: Undefined variable: g:baz")
  }

  @Test
  fun `case sensitive comparison right after a variable name still works`() {
    executeVimscript("let g:foo = 'abc'")
    assertCommandOutput("echo g:foo==#'abc'", "1")
  }
}
