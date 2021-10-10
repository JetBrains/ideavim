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

import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.jetbrains.plugins.ideavim.ex.evaluate
import org.junit.Test
import kotlin.test.assertEquals

class StringParsingTests {

  @Test
  fun `quoted string`() {
    assertEquals(
      VimString("oh, hi Mark"),
      VimscriptParser.parseExpression("\"oh, hi Mark\"")!!.evaluate()
    )
  }

  @Test
  fun `single quoted string`() {
    assertEquals(
      VimString("oh, hi Mark"),
      VimscriptParser.parseExpression("'oh, hi Mark'")!!.evaluate()
    )
  }

  @Test
  fun `escaped backslash in quoted string`() {
    assertEquals(
      VimString("oh, \\hi Mark"),
      VimscriptParser.parseExpression("\"oh, \\\\hi Mark\"")!!.evaluate()
    )
  }

  @Test
  fun `escaped quote quoted string`() {
    assertEquals(
      VimString("oh, hi \"Mark\""),
      VimscriptParser.parseExpression("\"oh, hi \\\"Mark\\\"\"")!!.evaluate()
    )
  }

  @Test
  fun `backslashes in single quoted string`() {
    assertEquals(
      VimString("oh, hi \\\\Mark\\"),
      VimscriptParser.parseExpression("'oh, hi \\\\Mark\\'")!!.evaluate()
    )
  }

  @Test
  fun `escaped single quote in single quoted string`() {
    assertEquals(
      VimString("oh, hi 'Mark'"),
      VimscriptParser.parseExpression("'oh, hi ''Mark'''")!!.evaluate()
    )
  }

  @Test
  fun `single quoted string inside a double quoted string`() {
    assertEquals(
      VimString(" :echo \"no mapping for 45\"<CR>"),
      VimscriptParser.parseExpression(
        """
         ' :echo "no mapping for ' . 45 . '"<CR>'
        """.trimIndent()
      )!!.evaluate()
    )
  }
}
