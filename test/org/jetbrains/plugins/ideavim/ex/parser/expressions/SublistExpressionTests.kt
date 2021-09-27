package org.jetbrains.plugins.ideavim.ex.parser.expressions

import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.SublistExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.Variable
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.jetbrains.plugins.ideavim.ex.evaluate
import org.junit.experimental.theories.DataPoints
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(Theories::class)
class SublistExpressionTests {

  companion object {
    @JvmStatic
    val values = listOf("", " ") @DataPoints get
  }

  @Theory
  fun `sublist with no range specified`(sp1: String, sp2: String) {
    val ex = VimscriptParser.parseExpression("1[$sp1:$sp2]")
    assertTrue(ex is SublistExpression)
    assertEquals(VimInt(1), ex.expression.evaluate())
    assertNull(ex.from)
    assertNull(ex.to)
  }

  @Theory
  fun `sublist with only start specified`(sp1: String, sp2: String, sp3: String) {
    val ex = VimscriptParser.parseExpression("'text'[${sp1}2$sp2:$sp3]")
    assertTrue(ex is SublistExpression)
    assertEquals(VimString("text"), ex.expression.evaluate())
    assertEquals(VimInt(2), ex.from!!.evaluate())
    assertNull(ex.to)
  }

  @Theory
  fun `sublist with only end specified`(sp1: String, sp2: String, sp3: String) {
    val ex = VimscriptParser.parseExpression("var[$sp1:${sp2}32$sp3]")
    assertTrue(ex is SublistExpression)
    assertTrue(ex.expression is Variable)
    assertEquals("var", (ex.expression as Variable).name)
    assertNull(ex.from)
    assertEquals(VimInt(32), ex.to!!.evaluate())
  }

  @Theory
  fun `sublist with range specified`(sp1: String, sp2: String, sp3: String, sp4: String) {
    val ex = VimscriptParser.parseExpression("[1, 2, 3, 4, 5, 6][${sp1}1$sp2:${sp3}4$sp4]")
    assertTrue(ex is SublistExpression)
    assertEquals(
      VimList(mutableListOf(VimInt(1), VimInt(2), VimInt(3), VimInt(4), VimInt(5), VimInt(6))),
      ex.expression.evaluate()
    )
    assertEquals(VimInt(1), ex.from!!.evaluate())
    assertEquals(VimInt(4), ex.to!!.evaluate())
  }

  @Theory
  fun `sublist with non int expressions in ranges`(sp1: String, sp2: String, sp3: String, sp4: String, sp5: String, sp6: String) {
    val ex = VimscriptParser.parseExpression("[1, 2, 3, 4, 5, 6][${sp1}1$sp2+${sp3}5$sp4:$sp5'asd'$sp6]")
    assertTrue(ex is SublistExpression)
    assertEquals(
      VimList(mutableListOf(VimInt(1), VimInt(2), VimInt(3), VimInt(4), VimInt(5), VimInt(6))),
      ex.expression.evaluate()
    )
    assertEquals(VimInt(6), ex.from!!.evaluate())
    assertEquals(VimString("asd"), ex.to!!.evaluate())
  }
}
