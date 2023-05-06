/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.parser.visitors

import com.intellij.openapi.diagnostic.logger
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.ex.ranges.Range.Companion.createRange
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.vimscript.model.commands.CallCommand
import com.maddyhome.idea.vim.vimscript.model.commands.Command
import com.maddyhome.idea.vim.vimscript.model.commands.DelfunctionCommand
import com.maddyhome.idea.vim.vimscript.model.commands.EchoCommand
import com.maddyhome.idea.vim.vimscript.model.commands.ExecuteCommand
import com.maddyhome.idea.vim.vimscript.model.commands.GlobalCommand
import com.maddyhome.idea.vim.vimscript.model.commands.GoToLineCommand
import com.maddyhome.idea.vim.vimscript.model.commands.LetCommand
import com.maddyhome.idea.vim.vimscript.model.commands.ShiftLeftCommand
import com.maddyhome.idea.vim.vimscript.model.commands.ShiftRightCommand
import com.maddyhome.idea.vim.vimscript.model.commands.SplitCommand
import com.maddyhome.idea.vim.vimscript.model.commands.SplitType
import com.maddyhome.idea.vim.vimscript.model.commands.SubstituteCommand
import com.maddyhome.idea.vim.vimscript.model.commands.UnknownCommand
import com.maddyhome.idea.vim.vimscript.model.commands.mapping.MapClearCommand
import com.maddyhome.idea.vim.vimscript.model.commands.mapping.MapCommand
import com.maddyhome.idea.vim.vimscript.model.commands.mapping.UnMapCommand
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.expressions.SimpleExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.AssignmentOperator
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.AssignmentOperator.Companion.getByValue
import com.maddyhome.idea.vim.vimscript.parser.generated.VimscriptBaseVisitor
import com.maddyhome.idea.vim.vimscript.parser.generated.VimscriptParser
import com.maddyhome.idea.vim.vimscript.parser.generated.VimscriptParser.CallCommandContext
import com.maddyhome.idea.vim.vimscript.parser.generated.VimscriptParser.DelfunctionCommandContext
import com.maddyhome.idea.vim.vimscript.parser.generated.VimscriptParser.EchoCommandContext
import com.maddyhome.idea.vim.vimscript.parser.generated.VimscriptParser.ExprContext
import com.maddyhome.idea.vim.vimscript.parser.generated.VimscriptParser.OtherCommandContext
import com.maddyhome.idea.vim.vimscript.parser.generated.VimscriptParser.RangeContext
import com.maddyhome.idea.vim.vimscript.parser.generated.VimscriptParser.RangeOffsetContext
import org.antlr.v4.runtime.ParserRuleContext
import java.util.stream.Collectors
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

internal object CommandVisitor : VimscriptBaseVisitor<Command>() {

  private val logger = logger<CommandVisitor>()
  private val expressionVisitor: ExpressionVisitor = ExpressionVisitor

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

  private fun parseRangeExpression(ctx: VimscriptParser.RangeExpressionContext?): Pair<String, Int> {
    val offset = parseRangeOffset(ctx?.rangeOffset())
    return if (ctx == null) {
      return Pair(".", offset)
    } else if (ctx.rangeMember() == null) {
      Pair(".", offset)
    } else if (ctx.rangeMember().search() == null || ctx.rangeMember().search().isEmpty()) {
      Pair(ctx.rangeMember().text, offset)
    } else {
      val memberString = ctx.rangeMember().search().joinToString("\u0000") { it.text }
      Pair(memberString, offset)
    }
  }

  private fun parseRangesUnit(ctx: VimscriptParser.RangeUnitContext): Array<Range> {
    val valueAndOffset = parseRangeExpression(ctx.rangeExpression())
    val move = ctx.rangeSeparator()?.text == ";"
    val ranges = createRange(valueAndOffset.first, valueAndOffset.second, move)
    if (ranges == null) {
      logger.warn("Could not create a range for node ${ctx.text}")
      throw ExException("Could not create a range ${ctx.text}")
    }
    return ranges
  }

  private fun parseRanges(ctx: RangeContext?): Ranges {
    val ranges = Ranges()
    if (ctx?.rangeUnit() != null) {
      for (unit in ctx.rangeUnit()) {
        ranges.addRange(parseRangesUnit(unit))
      }
    }
    return ranges
  }

  override fun visitLet1Command(ctx: VimscriptParser.Let1CommandContext): Command {
    val ranges: Ranges = parseRanges(ctx.range())
    val variable: Expression = expressionVisitor.visit(ctx.expr(0))
    val operator = getByValue(ctx.assignmentOperator().text)
    val expression: Expression = expressionVisitor.visit(ctx.expr(1))
    val command = LetCommand(ranges, variable, operator, expression, true)
    command.rangeInScript = ctx.getTextRange()
    return command
  }

  override fun visitLet2Command(ctx: VimscriptParser.Let2CommandContext): Command {
    val command = LetCommand(Ranges(), SimpleExpression(0), AssignmentOperator.ASSIGNMENT, SimpleExpression(0), false)
    command.rangeInScript = ctx.getTextRange()
    return command
  }

  override fun visitEchoCommand(ctx: EchoCommandContext): Command {
    val ranges: Ranges = parseRanges(ctx.range())
    val expressions = ctx.expr().stream()
      .map { tree: ExprContext ->
        expressionVisitor.visit(tree)
      }
      .collect(Collectors.toList())
    val command = EchoCommand(ranges, expressions)
    command.rangeInScript = ctx.getTextRange()
    return command
  }

  override fun visitCallCommand(ctx: CallCommandContext): Command {
    val ranges: Ranges = parseRanges(ctx.range())
    val functionCall = ExpressionVisitor.visit(ctx.expr())
    val command = CallCommand(ranges, functionCall)
    command.rangeInScript = ctx.getTextRange()
    return command
  }

  override fun visitDelfunctionCommand(ctx: DelfunctionCommandContext): DelfunctionCommand {
    val ranges: Ranges = parseRanges(ctx.range())
    val functionScope =
      if (ctx.functionScope() != null) Scope.getByValue(ctx.functionScope().text) else null
    val functionName = ctx.functionName().text
    val ignoreIfMissing = ctx.replace != null
    val command = DelfunctionCommand(ranges, functionScope, functionName, ignoreIfMissing)
    command.rangeInScript = ctx.getTextRange()
    return command
  }

  override fun visitGoToLineCommand(ctx: VimscriptParser.GoToLineCommandContext): Command {
    val ranges: Ranges
    if (ctx.range() != null) {
      ranges = parseRanges(ctx.range())
    } else {
      ranges = Ranges()
      ranges.addRange(
        createRange(ctx.shortRange().text, 0, false)
          ?: throw ExException("Could not create a range"),
      )
    }
    val command = GoToLineCommand(ranges)
    command.rangeInScript = ctx.getTextRange()
    return command
  }

  override fun visitCommandWithComment(ctx: VimscriptParser.CommandWithCommentContext): Command {
    val ranges = parseRanges(ctx.range())
    val commandName = ctx.name.text
    val argument = ctx.commandArgumentWithoutBars()?.text ?: ""
    return createCommandByCommandContext(ranges, argument, commandName, ctx)
  }

  override fun visitCommandWithoutComments(ctx: VimscriptParser.CommandWithoutCommentsContext): Command {
    val ranges = parseRanges(ctx.range())
    val commandName = ctx.name.text
    val argument = ctx.commandArgumentWithoutBars()?.text ?: ""
    return createCommandByCommandContext(ranges, argument, commandName, ctx)
  }

  override fun visitCommandWithBars(ctx: VimscriptParser.CommandWithBarsContext): Command {
    val ranges = parseRanges(ctx.range())
    val commandName = ctx.name.text
    val argument = ctx.commandArgumentWithBars()?.text ?: ""
    return createCommandByCommandContext(ranges, argument, commandName, ctx)
  }

  private fun createCommandByCommandContext(ranges: Ranges, argument: String, commandName: String, ctx: ParserRuleContext): Command {
    val command = when (getCommandByName(commandName)) {
      MapCommand::class -> MapCommand(ranges, argument, commandName)
      MapClearCommand::class -> MapClearCommand(ranges, argument, commandName)
      UnMapCommand::class -> UnMapCommand(ranges, argument, commandName)
      GlobalCommand::class -> {
        if (commandName.startsWith("v")) {
          GlobalCommand(ranges, argument, true)
        } else {
          if (argument.startsWith("!")) GlobalCommand(ranges, argument.substring(1), true) else GlobalCommand(ranges, argument, false)
        }
      }
      SplitCommand::class -> {
        if (commandName.startsWith("v")) {
          SplitCommand(ranges, argument, SplitType.VERTICAL)
        } else {
          SplitCommand(ranges, argument, SplitType.HORIZONTAL)
        }
      }
      SubstituteCommand::class -> SubstituteCommand(ranges, argument, commandName)
      else -> getCommandByName(commandName).primaryConstructor!!.call(ranges, argument)
    }
    command.rangeInScript = ctx.getTextRange()
    return command
  }

  override fun visitShiftLeftCommand(ctx: VimscriptParser.ShiftLeftCommandContext): ShiftLeftCommand {
    val ranges = parseRanges(ctx.range())
    val argument = (ctx.commandArgument?.text ?: "").trim()
    val length = ctx.lShift().text.length
    val command = ShiftLeftCommand(ranges, argument, length)
    command.rangeInScript = ctx.getTextRange()
    return command
  }

  override fun visitShiftRightCommand(ctx: VimscriptParser.ShiftRightCommandContext): ShiftRightCommand {
    val ranges = parseRanges(ctx.range())
    val argument = (ctx.commandArgument?.text ?: "").trim()
    val length = ctx.rShift().text.length
    val command = ShiftRightCommand(ranges, argument, length)
    command.rangeInScript = ctx.getTextRange()
    return command
  }

  override fun visitExecuteCommand(ctx: VimscriptParser.ExecuteCommandContext): ExecuteCommand {
    val ranges = parseRanges(ctx.range())
    val expressions = ctx.expr().stream()
      .map { tree: ExprContext ->
        expressionVisitor.visit(tree)
      }
      .collect(Collectors.toList())
    val command = ExecuteCommand(ranges, expressions)
    command.rangeInScript = ctx.getTextRange()
    return command
  }

  override fun visitLetCommand(ctx: VimscriptParser.LetCommandContext): Command {
    val command = com.maddyhome.idea.vim.vimscript.parser.VimscriptParser.parseLetCommand(ctx.text) ?: LetCommand(Ranges(), SimpleExpression(0), AssignmentOperator.ASSIGNMENT, SimpleExpression(0), false)
    command.rangeInScript = ctx.getTextRange()
    return command
  }

  override fun visitOtherCommand(ctx: OtherCommandContext): Command {
    val ranges: Ranges = parseRanges(ctx.range())
    val name = ctx.commandName().text
    val argument = ctx.commandArgumentWithBars()?.text ?: ""

    val alphabeticPart = name.split(Regex("\\P{Alpha}"))[0]
    if (setOf("s", "su", "sub", "subs", "subst", "substi", "substit", "substitu", "substitut", "substitut", "substitute").contains(alphabeticPart)) {
      val command = SubstituteCommand(ranges, name.replaceFirst(alphabeticPart, "") + argument, alphabeticPart)
      command.rangeInScript = ctx.getTextRange()
      return command
    }

    val command = UnknownCommand(ranges, name, argument)
    command.rangeInScript = ctx.getTextRange()
    return command
  }

  private fun getTextRange(ctx: ParserRuleContext): TextRange {
    val startOffset = ctx.start.startIndex
    val endOffset = ctx.stop.stopIndex + 1
    return TextRange(startOffset, endOffset)
  }

  private fun getCommandByName(commandName: String): KClass<out Command> {
    return injector.vimscriptParser.exCommands.getCommand(commandName)?.getKClass() ?: UnknownCommand::class
  }
}
