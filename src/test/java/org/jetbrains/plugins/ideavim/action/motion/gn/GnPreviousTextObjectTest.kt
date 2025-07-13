/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.action.motion.gn

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.Direction
import com.maddyhome.idea.vim.key.VimKeyStroke
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import javax.swing.KeyStroke

class GnPreviousTextObjectTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test delete word`() {
    doTestWithSearch(
      injector.parser.parseKeys("dgN"),
      """
      Hello, ${c}this is a test here
      """.trimIndent(),
      """
        Hello, this is a ${c} here
      """.trimIndent(),
    )
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test delete second word`() {
    doTestWithSearch(
      injector.parser.parseKeys("2dgN"),
      """
      Hello, this is a test here
      Hello, this is a test ${c}here
      """.trimIndent(),
      """
        Hello, this is a ${c} here
        Hello, this is a test here
      """.trimIndent(),
    )
  }

  @Test
  fun `test gn uses last used pattern not just search pattern`() {
    doTest(
      listOf("/is<CR>", ":s/test/tester/<CR>", "$", "dgN"),
      "Hello, ${c}this is a test here",
      "Hello, this is a ${c}er here",
      Mode.NORMAL(),
    )
  }

  private fun doTestWithSearch(keys: List<VimKeyStroke>, before: String, after: String) {
    configureByText(before)
    VimPlugin.getSearch().setLastSearchState("test", "", Direction.FORWARDS)
    typeText(keys)
    assertState(after)
    assertState(Mode.NORMAL())
  }
}
