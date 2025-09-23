/*
 * Copyright 2003-2023 The IdeaVim authors
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
import com.maddyhome.idea.vim.vimscript.model.expressions.VariableExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.BinaryOperator
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.jetbrains.plugins.ideavim.productForArguments
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertEquals

class LambdaTests {

  companion object {
    val values = listOf("", " ")

    @JvmStatic
    fun arg3(): List<Arguments> = productForArguments(values, values, values)

    @JvmStatic
    fun arg4(): List<Arguments> = productForArguments(values, values, values, values)

    @JvmStatic
    fun arg6(): List<Arguments> = productForArguments(values, values, values, values, values, values)
  }

  @ParameterizedTest
  @MethodSource("arg3")
  fun `lambda with no args test`(sp1: String, sp2: String, sp3: String) {
    val lambdaExpression = VimscriptParser.parseExpression("{$sp1->$sp2'error'$sp3}") as LambdaExpression
    assertEquals(0, lambdaExpression.args.size)
    assertEquals(SimpleExpression("error"), lambdaExpression.expr)
  }

  @ParameterizedTest
  @MethodSource("arg6")
  fun `lambda with multiple args test`(sp1: String, sp2: String, sp3: String, sp4: String, sp5: String, sp6: String) {
    val lambdaExpression =
      VimscriptParser.parseExpression("{${sp1}a$sp2,${sp3}b$sp4->${sp5}a+b$sp6}") as LambdaExpression
    assertEquals(listOf("a", "b"), lambdaExpression.args)
    assertEquals(
      BinExpression(VariableExpression(null, "a"), VariableExpression(null, "b"), BinaryOperator.ADDITION),
      lambdaExpression.expr
    )
  }

  @Test
  fun `lambda function call with no args test`() {
    val functionCall = VimscriptParser.parseExpression("{->'error'}()") as LambdaFunctionCallExpression
    assertEquals(0, functionCall.arguments.size)
    assertEquals(0, functionCall.lambda.args.size)
    assertEquals(SimpleExpression("error"), functionCall.lambda.expr)
  }

  @ParameterizedTest
  @MethodSource("arg4")
  fun `lambda function call with multiple args test`(sp1: String, sp2: String, sp3: String, sp4: String) {
    val functionCall =
      VimscriptParser.parseExpression("{->'error'}(${sp1}a$sp2,${sp3}b$sp4)") as LambdaFunctionCallExpression
    assertEquals(2, functionCall.arguments.size)
    assertEquals(listOf(VariableExpression(null, "a"), VariableExpression(null, "b")), functionCall.arguments)
    assertEquals(0, functionCall.lambda.args.size)
    assertEquals(SimpleExpression("error"), functionCall.lambda.expr)
  }
}
