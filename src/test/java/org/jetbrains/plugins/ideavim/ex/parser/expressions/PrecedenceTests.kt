/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.parser.expressions

import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.BinExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.CurlyBracesName
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.expressions.FalsyExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.FuncrefCallExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.FunctionCallExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.IndexedExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.SimpleExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.SublistExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.TernaryExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.UnaryExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.VariableExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.BinaryOperator
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.ex.evaluate
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

// These tests are based on the precedence table in the Vim documentation. See `:help expression-syntax`
// Expressions are split into groups with increasing significance. The first group `expr1` is the least significant and
// contains the ternary expression (`expr2 ? expr1 : expr1`). The last group `expr11` is the most significant and
// includes literals, variables and lambdas.
// The tests are grouped by each level of precedence and verify that the expressions in that group take precedence over
// the expressions in the group below, where appropriate.

open class ExpressionPrecedenceTest : VimTestCase("\n") {
  protected fun parseExpression(expression: String): Expression {
    return VimscriptParser.parseExpression(expression)!!
  }

  protected fun assertExpressionPrecedence(expression: String, expected: String) {
    val parsed = VimscriptParser.parseExpression(expression)
    assertEquals(expected, formatExpression(parsed!!))
  }

  /**
   * Format an expression into a string representation for testing. Parentheses are added to make the precedence clear.
   */
  protected fun formatExpression(expression: Expression, wrapComplexExpression: Boolean = false): String {
    return when (expression) {
      is SimpleExpression -> {
        if (expression.data is VimString) {
          "'${expression.data.toOutputString()}'"
        } else {
          expression.data.toOutputString()
        }
      }

      is VariableExpression -> formatExpression(expression.name)
      is CurlyBracesName -> expression.evaluate().toOutputString()
      is UnaryExpression -> buildString {
        if (wrapComplexExpression) append('(')
        append(expression.operator.value)
        append(formatExpression(expression.expression, true))
        if (wrapComplexExpression) append(')')
      }

      is BinExpression -> buildString {
        if (wrapComplexExpression) append('(')
        append(formatExpression(expression.left, true))
        if (expression.operator == BinaryOperator.CONCATENATION) {
          // We use concatenation for indexed dictinoary access, e.g. expr10.name
          append(expression.operator.value)
        }
        else {
          append(' ').append(expression.operator.value).append(' ')
        }
        append(formatExpression(expression.right, true))
        if (wrapComplexExpression) append(')')
      }

      is IndexedExpression -> buildString {
        if (wrapComplexExpression) append('(')
        append(formatExpression(expression.expression, true))
        append('[').append(formatExpression(expression.index)).append(']')
        if (wrapComplexExpression) append(')')
      }

      is SublistExpression -> buildString {
        if (wrapComplexExpression) append('(')
        append(formatExpression(expression.expression, true))
        append('[')
        expression.from?.let { append(formatExpression(it)) }
        append(" : ")
        expression.to?.let { append(formatExpression(it)) }
        append(']')
        if (wrapComplexExpression) append(')')
      }

      is TernaryExpression -> buildString {
        if (wrapComplexExpression) append('(')
        append(formatExpression(expression.condition, true))
        append(" ? ")
        append(formatExpression(expression.then, true))
        append(" : ")
        append(formatExpression(expression.otherwise, true))
        if (wrapComplexExpression) append(')')
      }

      is FalsyExpression -> buildString {
        if (wrapComplexExpression) append('(')
        append(formatExpression(expression.left, true))
        append(" ?? ")
        append(formatExpression(expression.right, true))
        if (wrapComplexExpression) append(')')
      }

      is FunctionCallExpression -> buildString {
        if (wrapComplexExpression) append('(')
        append(formatExpression(expression.functionName))
        append('(')
        expression.arguments.joinTo(this) { formatExpression(it) }
        append(')')
        if (wrapComplexExpression) append(')')
      }

      is FuncrefCallExpression -> buildString {
        if (wrapComplexExpression) append('(')
        append(formatExpression(expression.expression, true))
        append('(')
        expression.args.joinTo(this) { formatExpression(it) }
        append(')')
        if (wrapComplexExpression) append(')')
      }

      else -> {
        "unknown expression type: ${expression.javaClass.simpleName}"
      }
    }
  }
}

/**
 * Test expressions in `expr11` have higher precedence than expressions in `expr10`
 *
 * Most expressions in this group are literals or variables, so it's hard to have ambiguous precedence.
 */
class Expr11Tests : ExpressionPrecedenceTest() {
  @Test
  fun `test nested expression has precedence over indexed expression`() {
    val result = parseExpression("([1, 2, 3] + [4, 5, 6])[4]").evaluate() as VimInt
    assertEquals(VimInt(5), result)
  }
}

class Expr10Tests : ExpressionPrecedenceTest() {
  // expr10[expr1]
  @Test
  fun `test indexed expression has higher precedence than unary not`() {
    // Unary not is expr9
    assertExpressionPrecedence(expression = "!foo[12]", expected = "!(foo[12])")
  }

  @Test
  fun `test indexed expression is left associative`() {
    assertExpressionPrecedence(expression = "foo[12][23]", expected = "(foo[12])[23]")
  }

  // expr10[expr1 : expr1]
  @Test
  fun `test sublist expression has higher precedence than unary not`() {
    // Unary not is expr9
    // This would fail at evaluation time, since a sublist expression returns a list, but it should still parse
    assertExpressionPrecedence(expression = "!foo[1 : 2]", expected = "!(foo[1 : 2])")
  }

  @Test
  fun `test sublist expression is left associative`() {
    assertExpressionPrecedence(expression = "foo[1 : 2][2 : 3]", expected = "(foo[1 : 2])[2 : 3]")
  }

  // expr10.name
  @Disabled("Dotted expressions are ambiguous with concatenation operator")
  @Test
  fun `test dotted index expression has higher precedence than unary not`() {
    // Note that our current implementation is a special case of `expr7 . expr7`, which is an expr6 expression
    // TODO: With proper parsing, I suspect this should be !(foo['name'])
    assertExpressionPrecedence(expression = "!foo.name", expected = "!(foo.name)")
  }

  @Test
  fun `test dotted index expression is left associative`() {
    // This is rewritten to index expressions
    assertExpressionPrecedence(expression = "foo.name.other", expected = "(foo['name'])['other']")
  }

  // expr10(expr1, ...)
  @Test
  fun `test function call has higher precedence than unary not`() {
    assertExpressionPrecedence(expression = "!foo(12)", expected = "!(foo(12))")
  }

  @Test
  fun `test function call is left associative`() {
    assertExpressionPrecedence(expression = "foo(12)(34)", expected = "(foo(12))(34)")
  }

  // expr10->name(expr1, ...)
  @Test
  fun `test method call has higher precedence than unary not`() {
    // A method call is rewritten as a function call with the object as the first argument
    // The unary operator still applies to the function call, not the object
    assertExpressionPrecedence(expression = "!foo->thing()", expected = "!(thing(foo))")
  }

  @Test
  fun `test method call is left associative`() {
    assertExpressionPrecedence(expression = "foo->thing()->other()", expected = "other(thing(foo))")
  }
}

class Expr9Tests : ExpressionPrecedenceTest() {
  // We don't support expr8, which is a Vim9 type cast. We'll compare with expr7, which is multiply, divide, modulus
  // !expr9
  @Test
  fun `test logical not has higher precedence than multiplication`() {
    // echo !0 * 24 => 24
    // echo !(0 * 24) => 1
    // Therefore `echo !0 * 24` is equivalent to `echo (!0) * 24``
    assertExpressionPrecedence(expression = "!foo * bar", expected = "(!foo) * bar")
  }

  @Test
  fun `test unary minus applies to numeric constant not expression`() {
    assertExpressionPrecedence(expression = "-4->abs()", "abs(-4)")
  }

  @Test
  fun `test unary minus applies to expression if not numeric constant`() {
    assertExpressionPrecedence(expression = "-a->abs()", "-(abs(a))")
  }
}

class Expr7Tests : ExpressionPrecedenceTest() {
  // expr8 * expr8
  @Test
  fun `test multiplication has higher precedence than addition`() {
    // Addition
    assertExpressionPrecedence(expression = "2 * 3 + 4", expected = "(2 * 3) + 4")
    assertExpressionPrecedence(expression = "4 + 2 * 3", expected = "4 + (2 * 3)")
  }

  @Test
  fun `test multiplication is left associative`() {
    assertExpressionPrecedence(expression = "1 * 2 * 3", expected = "(1 * 2) * 3")
  }

  // Division and modulo are the same precedence and covered by the same rule
}

class Expr6Tests : ExpressionPrecedenceTest() {
  // expr7 + expr7
  @Test
  fun `test addition has higher precedence than bitwise shift`() {
    assertExpressionPrecedence(expression = "2 + 3 << 5", expected = "(2 + 3) << 5")
    assertExpressionPrecedence(expression = "5 << 2 + 3", expected = "5 << (2 + 3)")
  }

  @Test
  fun `test addition is left associative`() {
    assertExpressionPrecedence(expression = "1 + 2 + 3", expected = "(1 + 2) + 3")
  }

  // expr7 . expr7
  @Test
  fun `test string concatenation has higher precedence than equality`() {
    assertExpressionPrecedence(expression = "'abc' . 'def' == 'abcdef'", expected = "('abc'.'def') == 'abcdef'")
    assertExpressionPrecedence(expression = "'abcdef' == 'abc' . 'def'", expected = "'abcdef' == ('abc'.'def')")
  }

  @Test
  fun `test string concatenation is left associative`() {
    assertExpressionPrecedence(expression = "'abc' . 'def' . 'ghi'", expected = "('abc'.'def').'ghi'")
  }
}

class Expr5Tests : ExpressionPrecedenceTest() {
  // expr6 << expr6
  @Test
  fun `test bitwise shift has higher precedence than comparison`() {
    assertExpressionPrecedence(expression = "2 << 3 == 16", expected = "(2 << 3) == 16")
    assertExpressionPrecedence(expression = "16 == 2 << 3", expected = "16 == (2 << 3)")
  }

  @Test
  fun `test bitwise shift is left associative`() {
    assertExpressionPrecedence(expression = "2 << 3 << 4", expected = "(2 << 3) << 4")
  }
}

class Expr4Tests : ExpressionPrecedenceTest() {
  // expr5 == expr5
  @Test
  fun `test equality has higher precedence than logical AND`() {
    assertExpressionPrecedence(expression = "1 == 1 && 1", expected = "(1 == 1) && 1")
    assertExpressionPrecedence(expression = "1 && 1 == 1", expected = "1 && (1 == 1)")
  }

  @Test
  fun `test equality is left associative`() {
    assertExpressionPrecedence(expression = "1 == 1 == 1", expected = "(1 == 1) == 1")
  }

  // expr5 is expr5
  @Test
  fun `test is operator has higher precedence than logical AND`() {
    assertExpressionPrecedence(expression = "1 is 1 && 1", expected = "(1 is 1) && 1")
    assertExpressionPrecedence(expression = "1 && 1 is 1", expected = "1 && (1 is 1)")
  }

  // expr5 isnot expr5
  @Test
  fun `test isnot operator has higher precedence than logical AND`() {
    assertExpressionPrecedence(expression = "1 isnot 1 && 1", expected = "(1 isnot 1) && 1")
    assertExpressionPrecedence(expression = "1 && 1 isnot 1", expected = "1 && (1 isnot 1)")
  }

  // The other operators are handled by the same rule, so we don't test them here
}

class Expr3Tests : ExpressionPrecedenceTest() {
  // expr4 && expr4
  @Test
  fun `test logical AND has higher precedence than logical OR`() {
    assertExpressionPrecedence(expression = "1 && 1 || 1", expected = "(1 && 1) || 1")
    assertExpressionPrecedence(expression = "1 || 1 && 1", expected = "1 || (1 && 1)")
  }

  @Test
  fun `test logical AND is left associative`() {
    assertExpressionPrecedence(expression = "1 && 1 && 1", expected = "(1 && 1) && 1")
  }
}

class Expr2Tests : ExpressionPrecedenceTest() {
  // expr3 || expr3
  @Test
  fun `test logical OR has higher precedence than ternary`() {
    assertExpressionPrecedence(expression = "1 || 1 ? 2 : 3", expected = "(1 || 1) ? 2 : 3")
    assertExpressionPrecedence(expression = "1 ? 2 : 1 || 3", expected = "1 ? 2 : (1 || 3)")
  }

  @Test
  fun `test logical OR is left associative`() {
    assertExpressionPrecedence(expression = "1 || 1 || 1", expected = "(1 || 1) || 1")
  }

  @Test
  fun `test logical OR has higher precedence that falsy operator`() {
    assertExpressionPrecedence(expression = "1 || 1 ?? 2", expected = "(1 || 1) ?? 2")
    assertExpressionPrecedence(expression = "2 ?? 1 || 1", expected = "2 ?? (1 || 1)")
  }
}

class Expr1Tests : ExpressionPrecedenceTest() {
  // expr2 ? expr1 : expr1
  @Test
  fun `test ternary is right associative`() {
    // echo 1 == 1 ? 1 : 1 == 2 ? 2 : 3
    // echo 1 == 1 ? 1 : (1 == 2 ? 2 : 3)
    // See VIM-3835
    assertExpressionPrecedence(expression = "1 == 1 ? 1 : 1 == 2 ? 2 : 3", expected = "(1 == 1) ? 1 : ((1 == 2) ? 2 : 3)")
  }

  @Test
  fun `test falsy operator is right associative`() {
    // I don't know how to write an actual Vim expression that would demonstrate this, but falsy is right associative
    assertExpressionPrecedence(expression = "1 ?? 2 ?? 3", expected = "1 ?? (2 ?? 3)")
  }
}
