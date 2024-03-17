/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.parser.expressions

import com.maddyhome.idea.vim.vimscript.model.expressions.Register
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.junit.jupiter.api.Test
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
