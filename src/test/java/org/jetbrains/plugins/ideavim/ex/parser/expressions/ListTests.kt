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

import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFloat
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.jetbrains.plugins.ideavim.ex.evaluate
import org.junit.Test
import kotlin.test.assertEquals

class ListTests {

  @Test
  fun `empty list test`() {
    assertEquals(VimList(mutableListOf()), VimscriptParser.parseExpression("[]")!!.evaluate())
  }

  @Test
  fun `list of simple types test`() {
    assertEquals(
      VimList(
        mutableListOf(
          VimInt(1),
          VimFloat(4.6),
          VimString("bla bla"),
          VimList(mutableListOf(VimInt(5), VimInt(9))),
          VimDictionary(linkedMapOf(VimString("key") to VimString("value")))
        )
      ),
      VimscriptParser.parseExpression("[1, 4.6, 'bla bla', [5, 9], {'key' : 'value'}]")!!.evaluate()
    )
  }

  @Test
  fun `comma at the list end test`() {
    assertEquals(VimList(mutableListOf(VimInt(1), VimInt(2))), VimscriptParser.parseExpression("[1, 2,]")!!.evaluate())
  }
}
