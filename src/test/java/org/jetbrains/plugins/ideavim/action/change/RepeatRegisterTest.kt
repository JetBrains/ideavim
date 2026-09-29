/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change

import com.maddyhome.idea.vim.VimPlugin
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * `.` and an explicit register. The numbered-register increment (`:h redo-register`) is covered by
 * [RepeatChangeActionTest]; a named register was not.
 *
 * Verified against nvim: `"add` then `.` leaves register `a` holding the *second* deleted line, so
 * the repeat reuses the register the original change named.
 */
class RepeatRegisterTest : VimTestCase() {

  @Test
  fun `test dot reuses the register named by the original change`() {
    doTest(
      "\"add" + ".",
      """
        ${c}alpha line one
        beta line two
        gamma line three
      """.trimIndent(),
      "${c}gamma line three",
    )
    assertEquals("beta line two\n", VimPlugin.getRegister().getRegister('a')?.text)
  }
}
