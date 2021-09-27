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
}
