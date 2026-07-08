/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.functions

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

/**
 * Autoload function names such as `foo#bar#baz` must be preserved in full when parsing function
 * declarations and calls — the `#`-separated namespace prefix is part of the name, not decoration.
 *
 * IdeaVim currently truncates the name to its last segment (see
 * `ExpressionVisitor.visitFunctionCall`, which reads only `functionName` and drops the
 * `(anyCaseNameWithDigitsAndUnderscores NUM)*` prefix from the grammar). That truncation is a
 * prerequisite blocker for plugins whose public API is an autoload function, e.g. `textobj#user#plugin`.
 */
class AutoloadFunctionNameTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `namespaced function is callable by its full name`() {
    executeVimscript("function! foo#bar#baz()\n  return 'HELLO'\nendfunction", true)
    assertCommandOutput("echo foo#bar#baz()", "HELLO")
  }

  @Test
  fun `namespaced function is not resolvable by its last segment`() {
    executeVimscript("function! foo#bar#baz()\n  return 'HELLO'\nendfunction", true)
    // The bare last segment is a different name and must not resolve to the namespaced function.
    enterCommand("echo baz()")
    assertPluginErrorMessage("E117: Unknown function: baz")
  }

  @Test
  fun `functions sharing a last segment across namespaces are distinct`() {
    executeVimscript("function! foo#bar#baz()\n  return 'FOO'\nendfunction", true)
    executeVimscript("function! qux#baz()\n  return 'QUX'\nendfunction", true)
    assertCommandOutput("echo foo#bar#baz()", "FOO")
    assertCommandOutput("echo qux#baz()", "QUX")
  }

  @Test
  fun `delfunction removes a function by its full namespaced name`() {
    executeVimscript("function! foo#bar#baz()\n  return 'HELLO'\nendfunction", true)
    executeVimscript("delfunction foo#bar#baz", true)
    enterCommand("echo foo#bar#baz()")
    assertPluginErrorMessage("E117: Unknown function: foo#bar#baz")
  }
}
