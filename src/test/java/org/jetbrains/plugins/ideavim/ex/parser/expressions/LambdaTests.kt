/*
 * Copyright 2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.parser.expressions

import com.maddyhome.idea.vim.vimscript.model.expressions.BinExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.LambdaExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.LambdaFunctionCallExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.SimpleExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.Variable
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.BinaryOperator
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.junit.experimental.theories.DataPoints
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(Theories::class)
class LambdaTests {

  companion object {
    @JvmStatic
    val values = listOf("", " ") @DataPoints get
  }

  @Theory
  fun `lambda with no args test`(sp1: String, sp2: String, sp3: String) {
    val lambdaExpression = VimscriptParser.parseExpression("{$sp1->$sp2'error'$sp3}") as LambdaExpression
    assertEquals(0, lambdaExpression.args.size)
    assertEquals(SimpleExpression("error"), lambdaExpression.expr)
  }

  @Theory
  fun `lambda with multiple args test`(sp1: String, sp2: String, sp3: String, sp4: String, sp5: String, sp6: String) {
    val lambdaExpression = VimscriptParser.parseExpression("{${sp1}a$sp2,${sp3}b$sp4->${sp5}a+b$sp6}") as LambdaExpression
    assertEquals(listOf("a", "b"), lambdaExpression.args)
    assertEquals(BinExpression(Variable(null, "a"), Variable(null, "b"), BinaryOperator.ADDITION), lambdaExpression.expr)
  }

  @Theory
  fun `lambda function call with no args test`() {
    val functionCall = VimscriptParser.parseExpression("{->'error'}()") as LambdaFunctionCallExpression
    assertEquals(0, functionCall.arguments.size)
    assertEquals(0, functionCall.lambda.args.size)
    assertEquals(SimpleExpression("error"), functionCall.lambda.expr)
  }

  @Theory
  fun `lambda function call with multiple args test`(sp1: String, sp2: String, sp3: String, sp4: String) {
    val functionCall = VimscriptParser.parseExpression("{->'error'}(${sp1}a$sp2,${sp3}b$sp4)") as LambdaFunctionCallExpression
    assertEquals(2, functionCall.arguments.size)
    assertEquals(listOf(Variable(null, "a"), Variable(null, "b")), functionCall.arguments)
    assertEquals(0, functionCall.lambda.args.size)
    assertEquals(SimpleExpression("error"), functionCall.lambda.expr)
  }
}
