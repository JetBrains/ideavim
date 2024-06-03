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
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.ranges.Address
import com.maddyhome.idea.vim.ex.ranges.Address.Companion.createRangeAddresses
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.newapi.globalIjOptions
import com.maddyhome.idea.vim.vimscript.model.commands.ActionCommand
import com.maddyhome.idea.vim.vimscript.model.commands.ActionListCommand
import com.maddyhome.idea.vim.vimscript.model.commands.AsciiCommand
import com.maddyhome.idea.vim.vimscript.model.commands.BufferCloseCommand
import com.maddyhome.idea.vim.vimscript.model.commands.BufferCommand
import com.maddyhome.idea.vim.vimscript.model.commands.BufferListCommand
import com.maddyhome.idea.vim.ex.ranges.Range.Companion.createRange
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.parser.generated.VimscriptBaseVisitor
import com.maddyhome.idea.vim.parser.generated.VimscriptParser
import com.maddyhome.idea.vim.parser.generated.VimscriptParser.CallCommandContext
import com.maddyhome.idea.vim.parser.generated.VimscriptParser.DelfunctionCommandContext
import com.maddyhome.idea.vim.parser.generated.VimscriptParser.EchoCommandContext
import com.maddyhome.idea.vim.parser.generated.VimscriptParser.ExprContext
import com.maddyhome.idea.vim.parser.generated.VimscriptParser.OtherCommandContext
import com.maddyhome.idea.vim.parser.generated.VimscriptParser.RangeContext
import com.maddyhome.idea.vim.parser.generated.VimscriptParser.RangeOffsetContext
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
import org.antlr.v4.runtime.ParserRuleContext
import java.util.stream.Collectors
import kotlin.reflect.KClass
import kotlin.reflect.full.createType
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

  private fun parseRangeUnit(ctx: VimscriptParser.RangeUnitContext): Array<Address> {
    val valueAndOffset = parseRangeExpression(ctx.rangeExpression())
    val move = ctx.rangeSeparator()?.text == ";"
    val addresses = createRangeAddresses(valueAndOffset.first, valueAndOffset.second, move)
    if (addresses == null) {
      logger.warn("Could not create an address for node ${ctx.text}")
      throw ExException("Could not create an address ${ctx.text}")
    }
    return addresses
  }

  private fun parseRange(ctx: RangeContext?): Range {
    val range = Range()
    if (ctx?.rangeUnit() != null) {
      val addresses = ctx.rangeUnit()
      for (unit in addresses) {
        range.addAddresses(parseRangeUnit(unit))
      }
      // If the range ends with a dangling separator, the last address is an implied current line address
      if (addresses.last().rangeSeparator()?.text == ",") {
        createRangeAddresses(".", 0, false)?.let { range.addAddresses(it) }
      }
    }
    return range
  }

  override fun visitLet1Command(ctx: VimscriptParser.Let1CommandContext): Command {
    val range: Range = parseRange(ctx.range())
    val variable: Expression = expressionVisitor.visit(ctx.expr(0))
    val operator = getByValue(ctx.assignmentOperator().text)
    val expression: Expression = expressionVisitor.visit(ctx.expr(1))
    val command = LetCommand(range, variable, operator, expression, true)
    command.rangeInScript = ctx.getTextRange()
    return command
  }

  override fun visitLet2Command(ctx: VimscriptParser.Let2CommandContext): Command {
    val command = LetCommand(Range(), SimpleExpression(0), AssignmentOperator.ASSIGNMENT, SimpleExpression(0), false)
    command.rangeInScript = ctx.getTextRange()
    return command
  }

  override fun visitEchoCommand(ctx: EchoCommandContext): Command {
    val range: Range = parseRange(ctx.range())
    val expressions = ctx.expr().stream()
      .map { tree: ExprContext ->
        expressionVisitor.visit(tree)
      }
      .collect(Collectors.toList())
    val command = EchoCommand(range, expressions)
    command.rangeInScript = ctx.getTextRange()
    return command
  }

  override fun visitCallCommand(ctx: CallCommandContext): Command {
    val range: Range = parseRange(ctx.range())
    val functionCall = ExpressionVisitor.visit(ctx.expr())
    val command = CallCommand(range, functionCall)
    command.rangeInScript = ctx.getTextRange()
    return command
  }

  override fun visitDelfunctionCommand(ctx: DelfunctionCommandContext): DelfunctionCommand {
    val range: Range = parseRange(ctx.range())
    val functionScope =
      if (ctx.functionScope() != null) Scope.getByValue(ctx.functionScope().text) else null
    val functionName = ctx.functionName().text
    val ignoreIfMissing = ctx.replace != null
    val command = DelfunctionCommand(range, functionScope, functionName, ignoreIfMissing)
    command.rangeInScript = ctx.getTextRange()
    return command
  }

  override fun visitGoToLineCommand(ctx: VimscriptParser.GoToLineCommandContext): Command {
    val range: Range
    if (ctx.range() != null) {
      range = parseRange(ctx.range())
    } else {
      range = Range()
      range.addAddresses(
        createRangeAddresses(ctx.shortRange().text, 0, false)
          ?: throw ExException("Could not create a range"),
      )
    }
    val command = GoToLineCommand(range)
    command.rangeInScript = ctx.getTextRange()
    return command
  }

  override fun visitCommandWithComment(ctx: VimscriptParser.CommandWithCommentContext): Command {
    val ranges = parseRange(ctx.range())
    val commandName = ctx.name.text
    val argument = ctx.commandArgumentWithoutBars()?.text ?: ""
    return createCommandByCommandContext(ranges, argument, commandName, ctx)
  }

  override fun visitCommandWithoutComments(ctx: VimscriptParser.CommandWithoutCommentsContext): Command {
    val ranges = parseRange(ctx.range())
    val commandName = ctx.name.text
    val argument = ctx.commandArgumentWithoutBars()?.text ?: ""
    return createCommandByCommandContext(ranges, argument, commandName, ctx)
  }

  override fun visitCommandWithBars(ctx: VimscriptParser.CommandWithBarsContext): Command {
    val ranges = parseRange(ctx.range())
    val commandName = ctx.name.text
    val argument = ctx.commandArgumentWithBars()?.text ?: ""
    return createCommandByCommandContext(ranges, argument, commandName, ctx)
  }

  private fun createCommandByCommandContext(range: Range, argument: String, commandName: String, ctx: ParserRuleContext): Command {
    val command = when (getCommandByName(commandName)) {
      MapCommand::class -> MapCommand(range, argument, commandName)
      MapClearCommand::class -> MapClearCommand(range, argument, commandName)
      UnMapCommand::class -> UnMapCommand(range, argument, commandName)
      GlobalCommand::class -> {
        if (commandName.startsWith("v")) {
          GlobalCommand(range, argument, true)
        } else {
          if (argument.startsWith("!")) GlobalCommand(range, argument.substring(1), true) else GlobalCommand(range, argument, false)
        }
      }
      SplitCommand::class -> {
        if (commandName.startsWith("v")) {
          SplitCommand(range, argument, SplitType.VERTICAL)
        } else {
          SplitCommand(range, argument, SplitType.HORIZONTAL)
        }
      }
      SubstituteCommand::class -> SubstituteCommand(range, argument, commandName)
      else -> getCommandByName(commandName).primaryConstructor!!.call(range, argument)
    }
    command.rangeInScript = ctx.getTextRange()
    return command
  }

  override fun visitShiftLeftCommand(ctx: VimscriptParser.ShiftLeftCommandContext): ShiftLeftCommand {
    val ranges = parseRange(ctx.range())
    val argument = ctx.commandArgumentWithoutBars()?.text ?: ""
    val length = ctx.lShift().text.length
    val command = ShiftLeftCommand(ranges, argument, length)
    command.rangeInScript = ctx.getTextRange()
    return command
  }

  override fun visitShiftRightCommand(ctx: VimscriptParser.ShiftRightCommandContext): ShiftRightCommand {
    val ranges = parseRange(ctx.range())
    val argument = ctx.commandArgumentWithoutBars()?.text ?: ""
    val length = ctx.rShift().text.length
    val command = ShiftRightCommand(ranges, argument, length)
    command.rangeInScript = ctx.getTextRange()
    return command
  }

  override fun visitExecuteCommand(ctx: VimscriptParser.ExecuteCommandContext): ExecuteCommand {
    val ranges = parseRange(ctx.range())
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
    val command = com.maddyhome.idea.vim.vimscript.parser.VimscriptParser.parseLetCommand(ctx.text) ?: LetCommand(Range(), SimpleExpression(0), AssignmentOperator.ASSIGNMENT, SimpleExpression(0), false)
    command.rangeInScript = ctx.getTextRange()
    return command
  }

  override fun visitOtherCommand(ctx: OtherCommandContext): Command {
    val range: Range = parseRange(ctx.range())
    val name = ctx.commandName().text
    val argument = ctx.commandArgumentWithBars()?.text ?: ""

    val alphabeticPart = name.split(Regex("\\P{Alpha}"))[0]
    if (setOf("s", "su", "sub", "subs", "subst", "substi", "substit", "substitu", "substitut", "substitut", "substitute").contains(alphabeticPart)) {
      val command = SubstituteCommand(range, name.replaceFirst(alphabeticPart, "") + argument, alphabeticPart)
      command.rangeInScript = ctx.getTextRange()
      return command
    }
    val commandConstructor = getCommandByName(name).constructors
      .filter { it.parameters.size == 2 }
      .firstOrNull { it.parameters[0].type == Range::class.createType() && it.parameters[1].type == String::class.createType() }
    val command = commandConstructor?.call(range, argument) ?: UnknownCommand(range, name, argument)
    command.rangeInScript = ctx.getTextRange()
    return command
  }

  private fun getCommandByName(commandName: String): KClass<out Command> {
    return injector.vimscriptParser.exCommands.getCommand(commandName)?.getKClass() ?: UnknownCommand::class
  }
}
