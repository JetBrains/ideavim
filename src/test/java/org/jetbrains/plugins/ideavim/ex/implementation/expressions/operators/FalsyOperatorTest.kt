/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.expressions.operators

import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.ex.evaluate
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class FalsyOperatorTest : VimTestCase() {

  @Test
  fun `left expression is true`() {
    assertEquals(VimInt("42"), VimscriptParser.parseExpression("42 ?? 999")!!.evaluate())
  }

  @Test
  fun `left expression is false`() {
    assertEquals(VimInt("42"), VimscriptParser.parseExpression("0 ?? 42")!!.evaluate())
  }

  @Test
  fun `empty list as a left expression`() {
    assertEquals(VimString("list is empty"), VimscriptParser.parseExpression("[] ?? 'list is empty'")!!.evaluate())
  }

  @Test
  fun `nonempty list as a left expression`() {
    assertEquals(
      VimList(mutableListOf(VimInt(1), VimInt(2), VimInt(3))),
      VimscriptParser.parseExpression("[1, 2, 3] ?? 'list is empty'")!!.evaluate(),
    )
  }
}
