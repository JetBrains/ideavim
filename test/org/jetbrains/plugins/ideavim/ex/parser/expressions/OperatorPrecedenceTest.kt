/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package org.jetbrains.plugins.ideavim.ex.parser.expressions

import com.maddyhome.idea.vim.vimscript.model.VimContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.junit.Test
import kotlin.test.assertEquals

class OperatorPrecedenceTest {

  @Test
  fun `boolean operators`() {
    assertEquals(VimInt(0), VimscriptParser.parseExpression("0 || 1 && 0").evaluate(null, null, VimContext()))
  }

  @Test
  fun `boolean operators 2`() {
    assertEquals(VimInt(0), VimscriptParser.parseExpression("!1 || 0").evaluate(null, null, VimContext()))
  }

  @Test
  fun `concatenation and multiplication`() {
    assertEquals(VimString("410"), VimscriptParser.parseExpression("4 . 5 * 2").evaluate(null, null, VimContext()))
  }

  @Test
  fun `concatenation and multiplication 2`() {
    assertEquals(VimString("202"), VimscriptParser.parseExpression("4 * 5 . 2").evaluate(null, null, VimContext()))
  }

  @Test
  fun `arithmetic operators`() {
    assertEquals(VimInt(6), VimscriptParser.parseExpression("2 + 2 * 2").evaluate(null, null, VimContext()))
  }

  @Test
  fun `comparison operators`() {
    assertEquals(VimInt(1), VimscriptParser.parseExpression("10 < 5 + 29").evaluate(null, null, VimContext()))
  }
}
