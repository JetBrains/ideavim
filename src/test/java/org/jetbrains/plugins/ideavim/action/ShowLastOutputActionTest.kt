/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action

import com.maddyhome.idea.vim.api.injector
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import kotlin.test.assertEquals

class ShowLastOutputActionTest : VimTestCase("\n") {
  @Test
  fun `test repeats last command output`() {
    enterCommand("echo 'hello'")
    typeText("g<")
    val output = injector.outputPanel.getCurrentOutputPanel()
    assertNotNull(output)
    assertEquals("hello", output.text)
  }

  @Test
  fun `test repeats last error output`() {
    enterSearch("foo")
    val output = injector.outputPanel.getCurrentOutputPanel()
    assertNotNull(output)
    assertEquals("E486: Pattern not found: foo", output.text)
  }
}
