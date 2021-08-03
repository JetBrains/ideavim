package org.jetbrains.plugins.ideavim.ex.parser.expressions

import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.FunctionCallExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.jetbrains.plugins.ideavim.ex.evaluate
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FunctionCallExpressionTests {

  @Test
  fun `function call with no arguments`() {
    val ex = VimscriptParser.parseExpression("doSomething()")
    assertTrue(ex is FunctionCallExpression)
    assertEquals("doSomething", ex.functionName)
    assertNull(ex.scope)
    assertEquals(0, ex.arguments.size)
  }

  @Test
  fun `scoped function call`() {
    val ex = VimscriptParser.parseExpression("s:doSomething()")
    assertTrue(ex is FunctionCallExpression)
    assertEquals("doSomething", ex.functionName)
    assertNotNull(ex.scope)
    assertEquals(Scope.SCRIPT_VARIABLE, ex.scope)
    assertEquals(0, ex.arguments.size)
  }

  @Test
  fun `function call with simple arguments`() {
    val ex = VimscriptParser.parseExpression("f(0, 'string')")
    assertTrue(ex is FunctionCallExpression)
    assertEquals("f", ex.functionName)
    assertNull(ex.scope)
    assertNotNull(ex.arguments)
    assertEquals(2, ex.arguments.size)
    assertEquals(VimInt(0), ex.arguments[0].evaluate())
    assertEquals(VimString("string"), ex.arguments[1].evaluate())
  }
}
