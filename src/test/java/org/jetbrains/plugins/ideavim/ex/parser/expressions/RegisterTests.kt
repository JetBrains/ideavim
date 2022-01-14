/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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

import com.maddyhome.idea.vim.vimscript.model.expressions.Register
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.junit.Test
import kotlin.test.assertEquals

class RegisterTests {

//  @Test
//  // todo
//  fun `empty register`() {
//    assertEquals(Register(""), VimscriptParser.parseExpression("@"))
//  }

  @Test
  fun `non-empty register`() {
    assertEquals(Register('s'), VimscriptParser.parseExpression("@s"))
  }

  @Test
  fun `unnamed register`() {
    assertEquals(Register('@'), VimscriptParser.parseExpression("@@"))
  }
}
