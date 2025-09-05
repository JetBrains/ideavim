/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.parser.expressions

import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.expressions.Variable
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.jetbrains.plugins.ideavim.ex.evaluate
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VariableParsingTests {

  @Test
  fun variableTest() {
    val variable = VimscriptParser.parseExpression("variableName")
    assertTrue(variable is Variable)
    assertTrue(variable.scope == null)
    assertEquals("variableName", variable.name.evaluate().value)
  }

  @Test
  fun variableTest2() {
    val variable = VimscriptParser.parseExpression("t:variableName")
    assertTrue(variable is Variable)
    assertEquals(Scope.TABPAGE_VARIABLE, variable.scope)
    assertEquals("variableName", variable.name.evaluate().value)
  }
}
