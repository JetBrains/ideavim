package com.maddyhome.idea.vim.vimscript.parser.visitors

import com.maddyhome.idea.vim.ex.ExCommand
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.ex.ranges.Range.Companion.createRange
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.vimscript.model.Executable
import com.maddyhome.idea.vim.vimscript.model.commands.Command
import com.maddyhome.idea.vim.vimscript.model.commands.DelfunctionCommand
import com.maddyhome.idea.vim.vimscript.model.commands.EchoCommand
import com.maddyhome.idea.vim.vimscript.model.commands.GoToLineCommand
import com.maddyhome.idea.vim.vimscript.model.commands.LetCommand
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.AssignmentOperator.Companion.getByValue
import com.maddyhome.idea.vim.vimscript.parser.generated.VimscriptBaseVisitor
import com.maddyhome.idea.vim.vimscript.parser.generated.VimscriptParser
import com.maddyhome.idea.vim.vimscript.parser.generated.VimscriptParser.DelfunctionCommandContext
import com.maddyhome.idea.vim.vimscript.parser.generated.VimscriptParser.EchoCommandContext
import com.maddyhome.idea.vim.vimscript.parser.generated.VimscriptParser.ExprContext
import com.maddyhome.idea.vim.vimscript.parser.generated.VimscriptParser.LetCommandContext
import com.maddyhome.idea.vim.vimscript.parser.generated.VimscriptParser.OtherCommandContext
import com.maddyhome.idea.vim.vimscript.parser.generated.VimscriptParser.RangeContext
import com.maddyhome.idea.vim.vimscript.parser.generated.VimscriptParser.RangeOffsetContext
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.ErrorNode
import org.antlr.v4.runtime.tree.ParseTree
import java.util.stream.Collectors

object CommandVisitor : VimscriptBaseVisitor<Executable>() {

  private val expressionVisitor: ExpressionVisitor = ExpressionVisitor

  private fun nameFromContext(ctx: ParserRuleContext): String {
    val stringBuffer = StringBuffer()
    for (child in ctx.children) {
      if (child !is ErrorNode) {
        stringBuffer.append(child.text)
      }
    }
    return stringBuffer.toString()
  }

  private fun parseRangeOffset(ctx: RangeOffsetContext?): Int {
    var offset = 0
    if (ctx != null) {
      offset += ctx.plusOneOffset().size
      offset -= ctx.minusOneOffset().size
      for (number in ctx.numberInOffset()) {
        offset += Integer.parseInt(number.text)
      }
    }
    return offset
  }

  private fun parseRanges(ctx: RangeContext?): Ranges {
    val ranges = Ranges()
    if (ctx?.children != null) {
      var unprocessedRange: Pair<String, Int>? = null
      for (child in ctx.children) {
        if (child is VimscriptParser.RangeExpressionContext && child.children != null) {
          if (child.rangeMember() == null || (
            child.rangeMember().search() == null || child.rangeMember().search()
              .isEmpty()
            )
          ) {
            val memberString = if (child.rangeMember() == null) {
              "."
            } else {
              child.rangeMember().text
            }
            unprocessedRange = Pair(memberString, parseRangeOffset(child.rangeOffset()))
          } else {
            val memberString = child.rangeMember().search().map { it.text }.joinToString("\u0000")
            unprocessedRange = Pair(memberString, parseRangeOffset(child.rangeOffset()))
          }
        } else if (child is VimscriptParser.RangeSeparatorContext && child.children != null) {
          var range: Array<Range>?
          if (unprocessedRange != null) {
            range = createRange(
              unprocessedRange.first,
              unprocessedRange.second,
              child.text == ";"
            )
            unprocessedRange = null
          } else {
            range = createRange(".", 0, child.text == ";")
          }
          if (range != null) {
            ranges.addRange(range)
          } else {
            throw RuntimeException("Could not create range")
          }
        } else {
          continue
        }
      }
      if (unprocessedRange != null) {
        val range = createRange(unprocessedRange.first, unprocessedRange.second, false)
        if (range != null) {
          ranges.addRange(range)
        } else {
          throw RuntimeException("Could not create range")
        }
      }
      if (ctx.children[ctx.childCount - 1] is VimscriptParser.RangeSeparatorContext) {
        val range = createRange(".", 0, false)
        if (range != null) {
          ranges.addRange(range)
        } else {
          throw RuntimeException("Could not create range")
        }
        ranges.addRange(range)
      }
    }
    return ranges
  }

  override fun visitLetCommand(ctx: LetCommandContext): Command {
    val ranges: Ranges = parseRanges(ctx.range())
    val variable: Expression = expressionVisitor.visit(ctx.expr(0))
    val operator = getByValue(ctx.assignmentOperator().text)
    val expression: Expression = expressionVisitor.visit(ctx.expr(1))
    return LetCommand(ranges, variable, operator, expression, nameFromContext(ctx))
  }

  override fun visitEchoCommand(ctx: EchoCommandContext): Command {
    val ranges: Ranges = parseRanges(ctx.range())
    val expressions = ctx.expr().stream()
      .map { tree: ExprContext ->
        expressionVisitor.visit(tree)
      }
      .collect(Collectors.toList())
    return EchoCommand(ranges, expressions, nameFromContext(ctx))
  }

  override fun visitDelfunctionCommand(ctx: DelfunctionCommandContext): Executable {
    val ranges: Ranges = parseRanges(ctx.range())
    val functionScope =
      if (ctx.functionRef().anyScope() != null) Scope.getByValue(ctx.functionRef().anyScope().text) else null
    val functionName = ctx.functionRef().functionName().text
    val ignoreIfMissing = ctx.replace().childCount > 0
    return DelfunctionCommand(ranges, functionScope, functionName, ignoreIfMissing, ctx.text)
  }

  override fun visitGoToLineCommand(ctx: VimscriptParser.GoToLineCommandContext): Command {
    val ranges: Ranges = parseRanges(ctx.range())
    return GoToLineCommand(ranges, nameFromContext(ctx))
  }

  override fun visitOtherCommand(ctx: OtherCommandContext): ExCommand {
    val ranges: Ranges = parseRanges(ctx.range())
    val name = ctx.commandName().text
    var args: String? = null
    if (ctx.commandArgument() != null && ctx.commandArgument().children != null) {
      args = ctx.commandArgument().children.stream()
        .map { obj: ParseTree -> obj.text }
        .collect(Collectors.joining())
    }
    return ExCommand(ranges, name, args ?: "", nameFromContext(ctx))
  }
}
