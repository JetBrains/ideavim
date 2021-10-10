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

package org.jetbrains.plugins.ideavim.ex.implementation.expressions.operators

import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.jetbrains.plugins.ideavim.ex.evaluate
import org.junit.Test
import kotlin.test.assertEquals

class FalsyOperatorTest {

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
      VimscriptParser.parseExpression("[1, 2, 3] ?? 'list is empty'")!!.evaluate()
    )
  }
}
