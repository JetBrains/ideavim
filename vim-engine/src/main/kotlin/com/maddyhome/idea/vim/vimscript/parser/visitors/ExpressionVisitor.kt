/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.parser.visitors

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.parser.generated.VimscriptBaseVisitor
import com.maddyhome.idea.vim.parser.generated.VimscriptParser
import com.maddyhome.idea.vim.parser.generated.VimscriptParser.AdditiveExpressionContext
import com.maddyhome.idea.vim.parser.generated.VimscriptParser.BlobExpressionContext
import com.maddyhome.idea.vim.parser.generated.VimscriptParser.ComparisonExpressionContext
import com.maddyhome.idea.vim.parser.generated.VimscriptParser.DictionaryExpressionContext
import com.maddyhome.idea.vim.parser.generated.VimscriptParser.EnvVariableExpressionContext
import com.maddyhome.idea.vim.parser.generated.VimscriptParser.FalsyExpressionContext
import com.maddyhome.idea.vim.parser.generated.VimscriptParser.FloatExpressionContext
import com.maddyhome.idea.vim.parser.generated.VimscriptParser.FunctionCallExpressionContext
import com.maddyhome.idea.vim.parser.generated.VimscriptParser.IndexedExpressionContext
import com.maddyhome.idea.vim.parser.generated.VimscriptParser.IntExpressionContext
import com.maddyhome.idea.vim.parser.generated.VimscriptParser.ListExpressionContext
import com.maddyhome.idea.vim.parser.generated.VimscriptParser.LiteralDictionaryExpressionContext
import com.maddyhome.idea.vim.parser.generated.VimscriptParser.LogicalAndExpressionContext
import com.maddyhome.idea.vim.parser.generated.VimscriptParser.LogicalOrExpressionContext
import com.maddyhome.idea.vim.parser.generated.VimscriptParser.MultiplicativeExpressionContext
import com.maddyhome.idea.vim.parser.generated.VimscriptParser.OptionExpressionContext
import com.maddyhome.idea.vim.parser.generated.VimscriptParser.RegisterExpressionContext
import com.maddyhome.idea.vim.parser.generated.VimscriptParser.StringExpressionContext
import com.maddyhome.idea.vim.parser.generated.VimscriptParser.SublistExpressionContext
import com.maddyhome.idea.vim.parser.generated.VimscriptParser.TernaryExpressionContext
import com.maddyhome.idea.vim.parser.generated.VimscriptParser.UnaryExpressionContext
import com.maddyhome.idea.vim.parser.generated.VimscriptParser.VariableContext
import com.maddyhome.idea.vim.parser.generated.VimscriptParser.VariableExpressionContext
import com.maddyhome.idea.vim.parser.generated.VimscriptParser.WrappedExpressionContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.expressions.BinExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.CurlyBracesName
import com.maddyhome.idea.vim.vimscript.model.expressions.DictionaryExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.EnvVariableExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.expressions.FalsyExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.FuncrefCallExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.FunctionCallExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.LambdaExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.LambdaFunctionCallExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.ListExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.IndexedExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.OptionExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.RegisterExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.expressions.ScopeExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.SimpleExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.SublistExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.TernaryExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.UnaryExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.VariableExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.BinaryOperator
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.UnaryOperator
import org.antlr.v4.runtime.ParserRuleContext

object ExpressionVisitor : VimscriptBaseVisitor<Expression>() {

  override fun visitDictionaryExpression(ctx: DictionaryExpressionContext): Expression {
    val dict: LinkedHashMap<Expression, Expression> = LinkedHashMap()
    for (dictEntry in ctx.dictionary().dictionaryEntry()) {
      dict[visit(dictEntry.expr(0))] = visit(dictEntry.expr(1))
    }
    val result = DictionaryExpression(dict)
    result.originalString = ctx.text
    return result
  }

  override fun visitLiteralDictionaryExpression(ctx: LiteralDictionaryExpressionContext): Expression {
    val dict: LinkedHashMap<Expression, Expression> = LinkedHashMap()
    for (dictEntry in ctx.literalDictionary().literalDictionaryEntry()) {
      dict[SimpleExpression(dictEntry.literalDictionaryKey().text)] = visit(dictEntry.expr())
    }
    val result = DictionaryExpression(dict)
    result.originalString = ctx.text
    return result
  }

  override fun visitIntExpression(ctx: IntExpressionContext): Expression {
    val result = SimpleExpression(VimInt.parseNumber(ctx.unsignedInt().text) ?: VimInt.ZERO)
    result.originalString = ctx.unsignedInt().text
    if (ctx.unaryOperator != null) {
      val unary = UnaryExpression(UnaryOperator.getByValue(ctx.unaryOperator.text), result)
      unary.originalString = ctx.text
      return unary
    }
    return result
  }

  override fun visitStringExpression(ctx: StringExpressionContext): Expression {
    var text = ctx.text
    val firstSymbol = text[0]
    if (firstSymbol == '"') {
      text = injector.parser.parseVimScriptString(text.substring(1, text.length - 1))
    } else if (firstSymbol == '\'') {
      text = text
        .substring(1, text.length - 1)
        .replace("''", "'")
    }
    val result = SimpleExpression(text)
    result.originalString = ctx.text
    return result
  }

  override fun visitListExpression(ctx: ListExpressionContext): Expression {
    val result = ListExpression((ctx.list().expr().map { visit(it) }.toMutableList()))
    result.originalString = ctx.text
    return result
  }

  override fun visitMultiplicativeExpression(ctx: MultiplicativeExpressionContext): Expression {
    val left = visit(ctx.expr(0))
    val right = visit(ctx.expr(1))
    val operatorString = ctx.multiplicativeOperator().text
    val operator = BinaryOperator.getByValue(operatorString) ?: throw RuntimeException()
    val result = BinExpression(left, right, operator)
    result.originalString = ctx.text
    return result
  }

  override fun visitAdditiveExpression(ctx: AdditiveExpressionContext): Expression {
    val leftExpression = visit(ctx.expr(0))
    val rightExpression = visit(ctx.expr(1))
    val rightText: String = ctx.expr(1).text
    val operatorString = ctx.additiveOperator().text

    val result =
      when {
        operatorString == "."
          && !containsSpaces(ctx)
          && evaluationResultCouldBeADictionary(leftExpression)
          && matchesLiteralDictionaryKey(rightText) -> {

          // Expr-entry: mydict.key
          val index = SimpleExpression(rightText)
          IndexedExpression(index, leftExpression)
        }

        operatorString == "-"
          && leftExpression is IndexedExpression
          && !containsSpaces(ctx)
          && matchesLiteralDictionaryKey(rightText) -> {

          // Parse dict.left-right as a key called `left-right`
          // `dict.left` is first parsed as the left expression (sublist), and `right` is the right expression (simple)
          // E.g. `let dict = {'first-key': 42, 'second-key' : {'third-key': 'oh, hi Mark'}} | echo dict.first-key`
          // TODO: Vim does not handle this case. E716: Key not present in Dictionary: "first"
          val postfix = "-$rightText"
          val newIndex = SimpleExpression((leftExpression.index as SimpleExpression).data.toVimString().value + postfix)
          IndexedExpression(newIndex, leftExpression.expression)
        }

        operatorString == "."
          && !containsSpaces(ctx)
          && evaluationResultCouldBeADictionary(leftExpression)
          && rightExpression is IndexedExpression && matchesLiteralDictionaryKey(rightExpression.expression.originalString) -> {

          // Accessing a value in a Dictionary that's an element of a Dictionary, accessed by name. Recursive
          // leftExpression is a possible Dictionary: `mydict` or `{...}`
          // rightExpression is an index
          // leftExpression.rightExpression.rightIndex or leftExpression.rightExpression[leftIndex]
          IndexedExpression(
            rightExpression.index,
            IndexedExpression(SimpleExpression(rightExpression.expression.originalString), leftExpression)
          )
        }

        operatorString == "."
          && !containsSpaces(ctx)
          && rightExpression is FunctionCallExpression
          && evaluationResultCouldBeADictionary(leftExpression) -> {

          // Dictionary-function: mydict.len()
          val index = rightExpression.functionName
          FuncrefCallExpression(IndexedExpression(index, leftExpression), rightExpression.arguments)
        }

        else -> {
          // `.` as string concatenation, `-` as subtraction
          val operator = BinaryOperator.getByValue(operatorString) ?: throw RuntimeException()
          BinExpression(leftExpression, rightExpression, operator)
        }
      }
    result.originalString = ctx.text
    return result
  }

  private fun containsSpaces(ctx: ParserRuleContext): Boolean {
    for (child in ctx.children) {
      if (child.text.isBlank()) return true
    }
    return false
  }

  private fun matchesLiteralDictionaryKey(string: String): Boolean {
    return string.matches(Regex("[a-zA-Z0-9_-]+"))
  }

  private fun evaluationResultCouldBeADictionary(ctx: Expression): Boolean {
    return when (ctx) {
      is ListExpression, is UnaryExpression -> false
      is SimpleExpression -> ctx.data is VimDictionary
      else -> true
    }
  }

  override fun visitComparisonExpression(ctx: ComparisonExpressionContext): Expression {
    val left = visit(ctx.expr(0))
    val right = visit(ctx.expr(1))
    val operatorString = ctx.comparisonOperator().text
    val operator = BinaryOperator.getByValue(operatorString) ?: throw RuntimeException()
    val result = BinExpression(left, right, operator)
    result.originalString = ctx.text
    return result
  }

  override fun visitLogicalAndExpression(ctx: LogicalAndExpressionContext): Expression {
    val left = visit(ctx.expr(0))
    val right = visit(ctx.expr(1))
    val operatorString = ctx.logicalAndOperator().text
    val operator = BinaryOperator.getByValue(operatorString) ?: throw RuntimeException()
    val result = BinExpression(left, right, operator)
    result.originalString = ctx.text
    return result
  }

  override fun visitLogicalOrExpression(ctx: LogicalOrExpressionContext): Expression {
    val left = visit(ctx.expr(0))
    val right = visit(ctx.expr(1))
    val operatorString = ctx.logicalOrOperator().text
    val operator = BinaryOperator.getByValue(operatorString) ?: throw RuntimeException()
    val result = BinExpression(left, right, operator)
    result.originalString = ctx.text
    return result
  }

  override fun visitUnaryExpression(ctx: UnaryExpressionContext): Expression {
    val expression = visit(ctx.expr())
    val operator = UnaryOperator.getByValue(ctx.getChild(0).text)
    val result = UnaryExpression(operator, expression)
    result.originalString = ctx.text
    return result
  }

  override fun visitFloatExpression(ctx: FloatExpressionContext): Expression {
    val result = SimpleExpression(ctx.unsignedFloat().text.toDouble())
    result.originalString = ctx.unsignedFloat().text
    if (ctx.unaryOperator != null) {
      val unary = UnaryExpression(UnaryOperator.getByValue(ctx.unaryOperator.text), result)
      unary.originalString = ctx.text
      return unary
    }
    return result
  }

  override fun visitVariableExpression(ctx: VariableExpressionContext): Expression {
    val result = visitVariable(ctx.variable())
    result.originalString = ctx.text
    return result
  }

  override fun visitWrappedExpression(ctx: WrappedExpressionContext): Expression? {
    val result = visit(ctx.expr())
    result.originalString = ctx.text
    return result
  }

  override fun visitOptionExpression(ctx: OptionExpressionContext): Expression {
    val scope = ctx.option().optionScope()?.anyScope()?.let {
      when {
        it.G_LOWERCASE() != null -> Scope.GLOBAL_VARIABLE
        it.L_LOWERCASE() != null -> Scope.LOCAL_VARIABLE
        else -> null
      }
    }
    val result = OptionExpression(scope, ctx.option().optionName().text)
    result.originalString = ctx.text
    return result
  }

  override fun visitTernaryExpression(ctx: TernaryExpressionContext): Expression {
    val condition = visit(ctx.expr(0))
    val then = visit(ctx.expr(1))
    val otherwise = visit(ctx.expr(2))
    val result = TernaryExpression(condition, then, otherwise)
    result.originalString = ctx.text
    return result
  }

  override fun visitFunctionAsMethodCall1(ctx: VimscriptParser.FunctionAsMethodCall1Context): FunctionCallExpression {
    val functionCall = visitFunctionCall(ctx.functionCall())
    functionCall.arguments.add(0, visit(ctx.expr()))
    functionCall.originalString = ctx.text
    return functionCall
  }

  override fun visitFunctionAsMethodCall2(ctx: VimscriptParser.FunctionAsMethodCall2Context): LambdaFunctionCallExpression {
    val lambda = visitLambda(ctx.lambda())
    val arguments = mutableListOf(visit(ctx.expr()))
    arguments.addAll(visitFunctionArgs(ctx.functionArguments()))
    val result = LambdaFunctionCallExpression(lambda, arguments)
    result.originalString = ctx.text
    return result
  }

  override fun visitFunctionCallExpression(ctx: FunctionCallExpressionContext): Expression {
    // This can be either a function call through an expression that resolves to a funcref (i.e. `expr10(expr1, ...)`)
    // or a function call through a name (i.e. `name(expr1, ...)` or `n{am}e(expr1, ...)` - see `expr11`).
    // When it's through a (curly braces) name, the context will have a functionCall(), otherwise, we'll have an expr()
    // As a reminder, a method call (`expr10->name(expr1, ...)`) is converted into a function call though a name:
    // `expr10.name(expr1, ...)`.
    val expr = ctx.expr()
    val arguments = ctx.functionArguments()
    val result = if (expr != null && arguments != null) {
      FuncrefCallExpression(visit(expr), visitFunctionArgs(arguments))
    }
    else {
      visitFunctionCall(ctx.functionCall())
    }
    result.originalString = ctx.text
    return result
  }

  override fun visitFunctionCall(ctx: VimscriptParser.FunctionCallContext): FunctionCallExpression {
    val functionName = visitCurlyBracesName(ctx.functionName().curlyBracesName())
    var scope: Scope? = null
    if (ctx.functionScope() != null) {
      scope = Scope.getByValue(ctx.functionScope().text)
    }
    val functionArguments = visitFunctionArgs(ctx.functionArguments()).toMutableList()
    val result = FunctionCallExpression(scope, functionName, functionArguments)
    result.originalString = ctx.text
    return result
  }

  override fun visitLambdaFunctionCallExpression(ctx: VimscriptParser.LambdaFunctionCallExpressionContext): LambdaFunctionCallExpression {
    val lambda = visitLambda(ctx.lambda())
    val arguments = visitFunctionArgs(ctx.functionArguments())
    val result = LambdaFunctionCallExpression(lambda, arguments)
    result.originalString = ctx.text
    return result
  }

  private fun visitFunctionArgs(args: VimscriptParser.FunctionArgumentsContext): List<Expression> {
    val result = mutableListOf<Expression>()
    for (arg in args.functionArgument()) {
      if (arg.anyScope() != null) {
        result.add(ScopeExpression(Scope.getByValue(arg.anyScope().text)!!))
      } else if (arg.expr() != null) {
        result.add(visit(arg.expr()))
      }
    }
    return result
  }

  override fun visitLambdaExpression(ctx: VimscriptParser.LambdaExpressionContext): Expression {
    val result = super.visitLambdaExpression(ctx)
    result.originalString = ctx.text
    return result
  }

  override fun visitLambda(ctx: VimscriptParser.LambdaContext): LambdaExpression {
    val arguments = ctx.argumentsDeclaration().variableName().map { it.text }
    val expr = visit(ctx.expr())
    val result = LambdaExpression(arguments, expr)
    result.originalString = ctx.text
    return result
  }

  override fun visitSublistExpression(ctx: SublistExpressionContext): Expression {
    val ex = visit(ctx.expr(0))
    val from = if (ctx.from != null) visit(ctx.from) else null
    val to = if (ctx.to != null) visit(ctx.to) else null
    val result = SublistExpression(from, to, ex)
    result.originalString = ctx.text
    return result
  }

  override fun visitIndexedExpression(ctx: IndexedExpressionContext): Expression {
    val ex = visit(ctx.expr(0))
    val fromTo = visit(ctx.expr(1))
    val result = IndexedExpression(fromTo, ex)
    result.originalString = ctx.text
    return result
  }

  override fun visitEnvVariableExpression(ctx: EnvVariableExpressionContext): Expression {
    val result = EnvVariableExpression(ctx.envVariable().envVariableName().text)
    result.originalString = ctx.text
    return result
  }

  override fun visitRegisterExpression(ctx: RegisterExpressionContext): Expression {
    // The grammar ensures that we have two characters, `@{register}` so this is safe
    val result = RegisterExpression(ctx.text[1])
    result.originalString = ctx.text
    return result
  }

  override fun visitVariable(ctx: VariableContext): VariableExpression {
    val scope = if (ctx.variableScope() == null) null else Scope.getByValue(ctx.variableScope().text)
    val result = VariableExpression(scope, visitCurlyBracesName(ctx.variableName().curlyBracesName()))
    result.originalString = ctx.text
    return result
  }

  override fun visitFalsyExpression(ctx: FalsyExpressionContext): Expression {
    val left = visit(ctx.expr(0))
    val right = visit(ctx.expr(1))
    val result = FalsyExpression(left, right)
    result.originalString = ctx.text
    return result
  }

  override fun visitCurlyBracesName(ctx: VimscriptParser.CurlyBracesNameContext): CurlyBracesName {
    val parts = ctx.element().map { if (it.expr() != null) visit(it.expr()) else SimpleExpression(it.text) }
    val result = CurlyBracesName(parts)
    result.originalString = ctx.text
    return result
  }

  override fun visitBlobExpression(ctx: BlobExpressionContext?): Expression {
    TODO()
  }
}
