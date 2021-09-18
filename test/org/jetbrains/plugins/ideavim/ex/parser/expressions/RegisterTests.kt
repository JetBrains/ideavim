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
