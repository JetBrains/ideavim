package org.jetbrains.plugins.ideavim.ex.parser.expressions

import com.maddyhome.idea.vim.vimscript.model.VimContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.junit.Test
import kotlin.test.assertEquals

class StringParsingTests {

  @Test
  fun `quoted string`() {
    assertEquals(
      VimString("oh, hi Mark"),
      VimscriptParser.parseExpression("\"oh, hi Mark\"").evaluate(null, null, VimContext())
    )
  }

  @Test
  fun `single quoted string`() {
    assertEquals(
      VimString("oh, hi Mark"),
      VimscriptParser.parseExpression("'oh, hi Mark'").evaluate(null, null, VimContext())
    )
  }

  @Test
  fun `escaped backslash in quoted string`() {
    assertEquals(
      VimString("oh, \\hi Mark"),
      VimscriptParser.parseExpression("\"oh, \\\\hi Mark\"").evaluate(null, null, VimContext())
    )
  }

  @Test
  fun `escaped quote quoted string`() {
    assertEquals(
      VimString("oh, hi \"Mark\""),
      VimscriptParser.parseExpression("\"oh, hi \\\"Mark\\\"\"").evaluate(null, null, VimContext())
    )
  }

  @Test
  fun `backslashes in single quoted string`() {
    assertEquals(
      VimString("oh, hi \\\\Mark\\"),
      VimscriptParser.parseExpression("'oh, hi \\\\Mark\\'").evaluate(null, null, VimContext())
    )
  }

  @Test
  fun `escaped single quote in single quoted string`() {
    assertEquals(
      VimString("oh, hi 'Mark'"),
      VimscriptParser.parseExpression("'oh, hi ''Mark'''").evaluate(null, null, VimContext())
    )
  }
}
