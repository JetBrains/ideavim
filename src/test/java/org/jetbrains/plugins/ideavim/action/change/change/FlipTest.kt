/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.change

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class FlipTest : VimTestCase() {

  @Test
  fun `test flip line when cursor inside word`() {
    val before = """The h${c}ello world""".trimIndent()
    configureByText(before)
    enterCommand("flip")
    val after = """The o${c}lleh world""".trimIndent()
    assertState(after)
  }
}