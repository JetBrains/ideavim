package org.jetbrains.plugins.ideavim.ex.parser.expressions

import com.maddyhome.idea.vim.vimscript.model.VimContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.SublistExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.Variable
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SublistExpressionTests {

  @Test
  fun `sublist with no range specified`() {
    val ex = VimscriptParser.parseExpression("1[:]")
    assertTrue(ex is SublistExpression)
    assertEquals(VimInt(1), ex.expression.evaluate(null, null, VimContext()))
    assertNull(ex.from)
    assertNull(ex.to)
  }

  @Test
  fun `sublist with only start specified`() {
    val ex = VimscriptParser.parseExpression("'text'[2:]")
    assertTrue(ex is SublistExpression)
    assertEquals(VimString("text"), ex.expression.evaluate(null, null, VimContext()))
    assertEquals(VimInt(2), ex.from!!.evaluate(null, null, VimContext()))
    assertNull(ex.to)
  }

  @Test
  fun `sublist with only end specified`() {
    val ex = VimscriptParser.parseExpression("var[:32]")
    assertTrue(ex is SublistExpression)
    assertTrue(ex.expression is Variable)
    assertEquals("var", (ex.expression as Variable).name)
    assertNull(ex.from)
    assertEquals(VimInt(32), ex.to!!.evaluate(null, null, VimContext()))
  }

  @Test
  fun `sublist with range specified`() {
    val ex = VimscriptParser.parseExpression("[1, 2, 3, 4, 5, 6][1:4]")
    assertTrue(ex is SublistExpression)
    assertEquals(
      VimList(mutableListOf(VimInt(1), VimInt(2), VimInt(3), VimInt(4), VimInt(5), VimInt(6))),
      ex.expression.evaluate(null, null, VimContext())
    )
    assertEquals(VimInt(1), ex.from!!.evaluate(null, null, VimContext()))
    assertEquals(VimInt(4), ex.to!!.evaluate(null, null, VimContext()))
  }

  @Test
  fun `sublist with non int expressions in ranges`() {
    val ex = VimscriptParser.parseExpression("[1, 2, 3, 4, 5, 6][ 1 + 5 : 'asd']")
    assertTrue(ex is SublistExpression)
    assertEquals(
      VimList(mutableListOf(VimInt(1), VimInt(2), VimInt(3), VimInt(4), VimInt(5), VimInt(6))),
      ex.expression.evaluate(null, null, VimContext())
    )
    assertEquals(VimInt(6), ex.from!!.evaluate(null, null, VimContext()))
    assertEquals(VimString("asd"), ex.to!!.evaluate(null, null, VimContext()))
  }
}
