/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.parser.expressions

import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.jetbrains.plugins.ideavim.ex.evaluate
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class OperatorPrecedenceTest {

  @Test
  fun `boolean operators`() {
    assertEquals(VimInt(0), VimscriptParser.parseExpression("0 || 1 && 0")!!.evaluate())
  }

  @Test
  fun `boolean operators 2`() {
    assertEquals(VimInt(0), VimscriptParser.parseExpression("!1 || 0")!!.evaluate())
  }

  @Test
  fun `concatenation and multiplication`() {
    assertEquals(VimString("410"), VimscriptParser.parseExpression("4 . 5 * 2")!!.evaluate())
  }

  @Test
  fun `concatenation and multiplication 2`() {
    assertEquals(VimString("202"), VimscriptParser.parseExpression("4 * 5 . 2")!!.evaluate())
  }

  @Test
  fun `arithmetic operators`() {
    assertEquals(VimInt(6), VimscriptParser.parseExpression("2 + 2 * 2")!!.evaluate())
  }

  @Test
  fun `comparison operators`() {
    assertEquals(VimInt(1), VimscriptParser.parseExpression("10 < 5 + 29")!!.evaluate())
  }

  @Test
  fun `sublist operator`() {
    assertEquals(VimString("ab"), VimscriptParser.parseExpression("'a' . 'bc'[0]")!!.evaluate())
  }

  @Test
  fun `not with sublist`() {
    assertEquals(VimInt(0), VimscriptParser.parseExpression("!{'a': 1}['a']")!!.evaluate())
  }
}
