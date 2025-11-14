/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.parser.expressions

import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.FunctionCallExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.expressions.ScopeExpression
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.ex.evaluate
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FunctionCallExpressionTests : VimTestCase() {

  @Test
  fun `function call with no arguments`() {
    val ex = VimscriptParser.parseExpression("doSomething()")
    assertInstanceOf<FunctionCallExpression>(ex)
    assertEquals("doSomething", ex.functionName.evaluate().value)
    assertNull(ex.scope)
    assertEquals(0, ex.arguments.size)
  }

  @Test
  fun `scoped function call`() {
    val ex = VimscriptParser.parseExpression("s:doSomething()")
    assertInstanceOf<FunctionCallExpression>(ex)
    assertEquals("doSomething", ex.functionName.evaluate().value)
    assertNotNull(ex.scope)
    assertEquals(Scope.SCRIPT_VARIABLE, ex.scope)
    assertEquals(0, ex.arguments.size)
  }

  @Test
  fun `function call with simple arguments`() {
    val ex = VimscriptParser.parseExpression("f(0, 'string')")
    assertInstanceOf<FunctionCallExpression>(ex)
    assertEquals("f", ex.functionName.evaluate().value)
    assertNull(ex.scope)
    assertNotNull(ex.arguments)
    assertEquals(2, ex.arguments.size)
    assertEquals(VimInt(0), ex.arguments[0].evaluate())
    assertEquals(VimString("string"), ex.arguments[1].evaluate())
  }

  @Test
  fun `scope as a function call argument`() {
    val ex = VimscriptParser.parseExpression("f(s:, 'string')")
    assertInstanceOf<FunctionCallExpression>(ex)
    assertEquals("f", ex.functionName.evaluate().value)
    assertNull(ex.scope)
    assertNotNull(ex.arguments)
    assertEquals(2, ex.arguments.size)
    assertEquals(ScopeExpression(Scope.SCRIPT_VARIABLE), ex.arguments[0])
    assertEquals(VimString("string"), ex.arguments[1].evaluate())
  }
}
