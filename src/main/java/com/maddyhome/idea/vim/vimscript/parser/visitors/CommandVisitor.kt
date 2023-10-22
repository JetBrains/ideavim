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
import com.maddyhome.idea.vim.newapi.globalIjOptions
import com.maddyhome.idea.vim.vimscript.model.commands.ActionCommand
import com.maddyhome.idea.vim.vimscript.model.commands.ActionListCommand
import com.maddyhome.idea.vim.vimscript.model.commands.AsciiCommand
import com.maddyhome.idea.vim.vimscript.model.commands.BufferCloseCommand
import com.maddyhome.idea.vim.vimscript.model.commands.BufferCommand
import com.maddyhome.idea.vim.vimscript.model.commands.BufferListCommand
import com.maddyhome.idea.vim.vimscript.model.commands.CallCommand
import com.maddyhome.idea.vim.vimscript.model.commands.CmdClearCommand
import com.maddyhome.idea.vim.vimscript.model.commands.CmdCommand
import com.maddyhome.idea.vim.vimscript.model.commands.CmdFilterCommand
import com.maddyhome.idea.vim.vimscript.model.commands.Command
import com.maddyhome.idea.vim.vimscript.model.commands.CopyTextCommand
import com.maddyhome.idea.vim.vimscript.model.commands.DelCmdCommand
import com.maddyhome.idea.vim.vimscript.model.commands.DeleteLinesCommand
import com.maddyhome.idea.vim.vimscript.model.commands.DeleteMarksCommand
import com.maddyhome.idea.vim.vimscript.model.commands.DelfunctionCommand
import com.maddyhome.idea.vim.vimscript.model.commands.DigraphCommand
import com.maddyhome.idea.vim.vimscript.model.commands.EchoCommand
import com.maddyhome.idea.vim.vimscript.model.commands.EditFileCommand
import com.maddyhome.idea.vim.vimscript.model.commands.ExecuteCommand
import com.maddyhome.idea.vim.vimscript.model.commands.ExitCommand
import com.maddyhome.idea.vim.vimscript.model.commands.FileCommand
import com.maddyhome.idea.vim.vimscript.model.commands.FindFileCommand
import com.maddyhome.idea.vim.vimscript.model.commands.GlobalCommand
import com.maddyhome.idea.vim.vimscript.model.commands.GoToLineCommand
import com.maddyhome.idea.vim.vimscript.model.commands.GotoCharacterCommand
import com.maddyhome.idea.vim.vimscript.model.commands.HelpCommand
import com.maddyhome.idea.vim.vimscript.model.commands.HistoryCommand
import com.maddyhome.idea.vim.vimscript.model.commands.JoinLinesCommand
import com.maddyhome.idea.vim.vimscript.model.commands.JumpsCommand
import com.maddyhome.idea.vim.vimscript.model.commands.LetCommand
import com.maddyhome.idea.vim.vimscript.model.commands.LockVarCommand
import com.maddyhome.idea.vim.vimscript.model.commands.MarkCommand
import com.maddyhome.idea.vim.vimscript.model.commands.MarksCommand
import com.maddyhome.idea.vim.vimscript.model.commands.MoveTextCommand
import com.maddyhome.idea.vim.vimscript.model.commands.NextFileCommand
import com.maddyhome.idea.vim.vimscript.model.commands.NextTabCommand
import com.maddyhome.idea.vim.vimscript.model.commands.NoHLSearchCommand
import com.maddyhome.idea.vim.vimscript.model.commands.NormalCommand
import com.maddyhome.idea.vim.vimscript.model.commands.OnlyCommand
import com.maddyhome.idea.vim.vimscript.model.commands.PackaddCommand
import com.maddyhome.idea.vim.vimscript.model.commands.PlugCommand
import com.maddyhome.idea.vim.vimscript.model.commands.PreviousFileCommand
import com.maddyhome.idea.vim.vimscript.model.commands.PreviousTabCommand
import com.maddyhome.idea.vim.vimscript.model.commands.PrintCommand
import com.maddyhome.idea.vim.vimscript.model.commands.PutLinesCommand
import com.maddyhome.idea.vim.vimscript.model.commands.QuitCommand
import com.maddyhome.idea.vim.vimscript.model.commands.RedoCommand
import com.maddyhome.idea.vim.vimscript.model.commands.RegistersCommand
import com.maddyhome.idea.vim.vimscript.model.commands.RepeatCommand
import com.maddyhome.idea.vim.vimscript.model.commands.SelectFileCommand
import com.maddyhome.idea.vim.vimscript.model.commands.SelectFirstFileCommand
import com.maddyhome.idea.vim.vimscript.model.commands.SelectLastFileCommand
import com.maddyhome.idea.vim.vimscript.model.commands.SetCommand
import com.maddyhome.idea.vim.vimscript.model.commands.SetHandlerCommand
import com.maddyhome.idea.vim.vimscript.model.commands.SetglobalCommand
import com.maddyhome.idea.vim.vimscript.model.commands.SetlocalCommand
import com.maddyhome.idea.vim.vimscript.model.commands.ShellCommand
import com.maddyhome.idea.vim.vimscript.model.commands.ShiftLeftCommand
import com.maddyhome.idea.vim.vimscript.model.commands.ShiftRightCommand
import com.maddyhome.idea.vim.vimscript.model.commands.SortCommand
import com.maddyhome.idea.vim.vimscript.model.commands.SourceCommand
import com.maddyhome.idea.vim.vimscript.model.commands.SplitCommand
import com.maddyhome.idea.vim.vimscript.model.commands.SplitType
import com.maddyhome.idea.vim.vimscript.model.commands.SubstituteCommand
import com.maddyhome.idea.vim.vimscript.model.commands.TabCloseCommand
import com.maddyhome.idea.vim.vimscript.model.commands.TabMoveCommand
import com.maddyhome.idea.vim.vimscript.model.commands.TabOnlyCommand
import com.maddyhome.idea.vim.vimscript.model.commands.UndoCommand
import com.maddyhome.idea.vim.vimscript.model.commands.UnknownCommand
import com.maddyhome.idea.vim.vimscript.model.commands.UnlockVarCommand
import com.maddyhome.idea.vim.vimscript.model.commands.WriteAllCommand
import com.maddyhome.idea.vim.vimscript.model.commands.WriteCommand
import com.maddyhome.idea.vim.vimscript.model.commands.WriteNextFileCommand
import com.maddyhome.idea.vim.vimscript.model.commands.WritePreviousFileCommand
import com.maddyhome.idea.vim.vimscript.model.commands.WriteQuitCommand
import com.maddyhome.idea.vim.vimscript.model.commands.YankLinesCommand
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
    val command = com.maddyhome.idea.vim.vimscript.parser.VimscriptParser.parseLetCommand(ctx.text)
      ?: LetCommand(Ranges(), SimpleExpression(0), AssignmentOperator.ASSIGNMENT, SimpleExpression(0), false)
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
    val commandConstructor = getCommandByName(name).constructors
      .filter { it.parameters.size == 2 }
      .firstOrNull { it.parameters[0].type == Ranges::class.createType() && it.parameters[1].type == String::class.createType() }
    val command = commandConstructor?.call(ranges, argument) ?: UnknownCommand(ranges, name, argument)
    command.rangeInScript = ctx.getTextRange()
    return command
  }

  private fun getTextRange(ctx: ParserRuleContext): TextRange {
    val startOffset = ctx.start.startIndex
    val endOffset = ctx.stop.stopIndex + 1
    return TextRange(startOffset, endOffset)
  }

  // todo I am ashamed of that
  // todo 31.05.2023 I am still ashamed of that
  private val commands = mutableMapOf(
    "delf" to DelfunctionCommand::class,
    "delfu" to DelfunctionCommand::class,
    "delfun" to DelfunctionCommand::class,
    "delfunc" to DelfunctionCommand::class,
    "delfunct" to DelfunctionCommand::class,
    "delfuncti" to DelfunctionCommand::class,
    "delfunctio" to DelfunctionCommand::class,
    "delfunction" to DelfunctionCommand::class,
    "action" to ActionCommand::class,
    "actionlist" to ActionListCommand::class,
    "as" to AsciiCommand::class,
    "asc" to AsciiCommand::class,
    "asci" to AsciiCommand::class,
    "ascii" to AsciiCommand::class,
    "b" to BufferCommand::class,
    "bu" to BufferCommand::class,
    "buf" to BufferCommand::class,
    "buff" to BufferCommand::class,
    "buffe" to BufferCommand::class,
    "buffer" to BufferCommand::class,
    "bd" to BufferCloseCommand::class,
    "bde" to BufferCloseCommand::class,
    "bdel" to BufferCloseCommand::class,
    "bdele" to BufferCloseCommand::class,
    "bdelet" to BufferCloseCommand::class,
    "bdelete" to BufferCloseCommand::class,
    "ls" to BufferListCommand::class,
    "files" to BufferListCommand::class,
    "buffers" to BufferListCommand::class,
    "cal" to CallCommand::class,
    "call" to CallCommand::class,
    "com" to CmdCommand::class,
    "comm" to CmdCommand::class,
    "comma" to CmdCommand::class,
    "comman" to CmdCommand::class,
    "command" to CmdCommand::class,
    "!" to CmdFilterCommand::class,
    "comc" to CmdClearCommand::class,
    "comcl" to CmdClearCommand::class,
    "comcle" to CmdClearCommand::class,
    "comclea" to CmdClearCommand::class,
    "comclear" to CmdClearCommand::class,
    "t" to CopyTextCommand::class,
    "co" to CopyTextCommand::class,
    "cop" to CopyTextCommand::class,
    "copy" to CopyTextCommand::class,
    "delc" to DelCmdCommand::class,
    "delco" to DelCmdCommand::class,
    "delcom" to DelCmdCommand::class,
    "delcomm" to DelCmdCommand::class,
    "delcomma" to DelCmdCommand::class,
    "delcomman" to DelCmdCommand::class,
    "delcommand" to DelCmdCommand::class,
    "d" to DeleteLinesCommand::class,
    "de" to DeleteLinesCommand::class,
    "del" to DeleteLinesCommand::class,
    "dele" to DeleteLinesCommand::class,
    "delet" to DeleteLinesCommand::class,
    "delete" to DeleteLinesCommand::class,
    "delm" to DeleteMarksCommand::class,
    "delma" to DeleteMarksCommand::class,
    "delmar" to DeleteMarksCommand::class,
    "delmark" to DeleteMarksCommand::class,
    "delmarks" to DeleteMarksCommand::class,
    "dig" to DigraphCommand::class,
    "digr" to DigraphCommand::class,
    "digra" to DigraphCommand::class,
    "digrap" to DigraphCommand::class,
    "digraph" to DigraphCommand::class,
    "digraphs" to DigraphCommand::class,
    "ec" to EchoCommand::class,
    "ech" to EchoCommand::class,
    "echo" to EchoCommand::class,
    "e" to EditFileCommand::class,
    "ed" to EditFileCommand::class,
    "edi" to EditFileCommand::class,
    "edit" to EditFileCommand::class,
    "bro" to EditFileCommand::class,
    "brow" to EditFileCommand::class,
    "brows" to EditFileCommand::class,
    "browse" to EditFileCommand::class,
    "wqa" to ExitCommand::class,
    "wqal" to ExitCommand::class,
    "wqall" to ExitCommand::class,
    "qa" to ExitCommand::class,
    "qal" to ExitCommand::class,
    "qall" to ExitCommand::class,
    "xa" to ExitCommand::class,
    "xal" to ExitCommand::class,
    "xall" to ExitCommand::class,
    "quita" to ExitCommand::class,
    "quital" to ExitCommand::class,
    "quitall" to ExitCommand::class,
    "f" to FileCommand::class,
    "fi" to FileCommand::class,
    "fil" to FileCommand::class,
    "file" to FileCommand::class,
    "fin" to FindFileCommand::class,
    "find" to FindFileCommand::class,
    "g" to GlobalCommand::class,
    "gl" to GlobalCommand::class,
    "glo" to GlobalCommand::class,
    "glob" to GlobalCommand::class,
    "globa" to GlobalCommand::class,
    "global" to GlobalCommand::class,
    "v" to GlobalCommand::class,
    "vg" to GlobalCommand::class,
    "vgl" to GlobalCommand::class,
    "vglo" to GlobalCommand::class,
    "vglob" to GlobalCommand::class,
    "vgloba" to GlobalCommand::class,
    "vglobal" to GlobalCommand::class,
    "go" to GotoCharacterCommand::class,
    "got" to GotoCharacterCommand::class,
    "goto" to GotoCharacterCommand::class,
    "h" to HelpCommand::class,
    "he" to HelpCommand::class,
    "hel" to HelpCommand::class,
    "help" to HelpCommand::class,
    "his" to HistoryCommand::class,
    "hist" to HistoryCommand::class,
    "histo" to HistoryCommand::class,
    "histor" to HistoryCommand::class,
    "history" to HistoryCommand::class,
    "j" to JoinLinesCommand::class,
    "jo" to JoinLinesCommand::class,
    "joi" to JoinLinesCommand::class,
    "join" to JoinLinesCommand::class,
    "ju" to JumpsCommand::class,
    "jum" to JumpsCommand::class,
    "jump" to JumpsCommand::class,
    "jumps" to JumpsCommand::class,
    "let" to LetCommand::class,
    "k" to MarkCommand::class,
    "ma" to MarkCommand::class,
    "mar" to MarkCommand::class,
    "mark" to MarkCommand::class,
    "marks" to MarksCommand::class,
    "m" to MoveTextCommand::class,
    "mo" to MoveTextCommand::class,
    "mov" to MoveTextCommand::class,
    "move" to MoveTextCommand::class,
    "n" to NextFileCommand::class,
    "ne" to NextFileCommand::class,
    "nex" to NextFileCommand::class,
    "next" to NextFileCommand::class,
    "bn" to NextFileCommand::class,
    "bne" to NextFileCommand::class,
    "bnex" to NextFileCommand::class,
    "bnext" to NextFileCommand::class,
    "tabn" to NextTabCommand::class,
    "tabne" to NextTabCommand::class,
    "tabnex" to NextTabCommand::class,
    "tabnext" to NextTabCommand::class,
    "noh" to NoHLSearchCommand::class,
    "nohl" to NoHLSearchCommand::class,
    "nohls" to NoHLSearchCommand::class,
    "nohlse" to NoHLSearchCommand::class,
    "nohlsea" to NoHLSearchCommand::class,
    "nohlsear" to NoHLSearchCommand::class,
    "nohlsearc" to NoHLSearchCommand::class,
    "nohlsearch" to NoHLSearchCommand::class,
    "norm" to NormalCommand::class,
    "norma" to NormalCommand::class,
    "normal" to NormalCommand::class,
    "on" to OnlyCommand::class,
    "onl" to OnlyCommand::class,
    "only" to OnlyCommand::class,
    "pa" to PackaddCommand::class,
    "pac" to PackaddCommand::class,
    "pack" to PackaddCommand::class,
    "packa" to PackaddCommand::class,
    "packad" to PackaddCommand::class,
    "packadd" to PackaddCommand::class,
    "Plug" to PlugCommand::class,
    "Plugi" to PlugCommand::class,
    "Plugin" to PlugCommand::class,
    "N" to PreviousFileCommand::class,
    "Ne" to PreviousFileCommand::class,
    "Nex" to PreviousFileCommand::class,
    "Next" to PreviousFileCommand::class,
    "prev" to PreviousFileCommand::class,
    "previ" to PreviousFileCommand::class,
    "previo" to PreviousFileCommand::class,
    "previou" to PreviousFileCommand::class,
    "previous" to PreviousFileCommand::class,
    "bp" to PreviousFileCommand::class,
    "bpr" to PreviousFileCommand::class,
    "bpre" to PreviousFileCommand::class,
    "bprev" to PreviousFileCommand::class,
    "bprevi" to PreviousFileCommand::class,
    "bprevio" to PreviousFileCommand::class,
    "bpreviou" to PreviousFileCommand::class,
    "bprevious" to PreviousFileCommand::class,
    "tabp" to PreviousTabCommand::class,
    "tabpr" to PreviousTabCommand::class,
    "tabpre" to PreviousTabCommand::class,
    "tabprev" to PreviousTabCommand::class,
    "tabprevi" to PreviousTabCommand::class,
    "tabprevio" to PreviousTabCommand::class,
    "tabpreviou" to PreviousTabCommand::class,
    "tabprevious" to PreviousTabCommand::class,
    "tabN" to PreviousTabCommand::class,
    "tabNe" to PreviousTabCommand::class,
    "tabNex" to PreviousTabCommand::class,
    "tabNext" to PreviousTabCommand::class,
    "p" to PrintCommand::class,
    "pr" to PrintCommand::class,
    "pri" to PrintCommand::class,
    "prin" to PrintCommand::class,
    "print" to PrintCommand::class,
    "P" to PrintCommand::class,
    "Pr" to PrintCommand::class,
    "Pri" to PrintCommand::class,
    "Prin" to PrintCommand::class,
    "Print" to PrintCommand::class,
    "pu" to PutLinesCommand::class,
    "put" to PutLinesCommand::class,
    "q" to QuitCommand::class,
    "qu" to QuitCommand::class,
    "qui" to QuitCommand::class,
    "quit" to QuitCommand::class,
    "clo" to QuitCommand::class,
    "clos" to QuitCommand::class,
    "close" to QuitCommand::class,
    "hid" to QuitCommand::class,
    "hide" to QuitCommand::class,
    "red" to RedoCommand::class,
    "redo" to RedoCommand::class,
    "di" to RegistersCommand::class,
    "dis" to RegistersCommand::class,
    "disp" to RegistersCommand::class,
    "displ" to RegistersCommand::class,
    "displa" to RegistersCommand::class,
    "display" to RegistersCommand::class,
    "exe" to ExecuteCommand::class,
    "exec" to ExecuteCommand::class,
    "execu" to ExecuteCommand::class,
    "execut" to ExecuteCommand::class,
    "execute" to ExecuteCommand::class,
    "reg" to RegistersCommand::class,
    "regi" to RegistersCommand::class,
    "regis" to RegistersCommand::class,
    "regist" to RegistersCommand::class,
    "registe" to RegistersCommand::class,
    "register" to RegistersCommand::class,
    "registers" to RegistersCommand::class,
    "@" to RepeatCommand::class,
    "argu" to SelectFileCommand::class,
    "argum" to SelectFileCommand::class,
    "argume" to SelectFileCommand::class,
    "argumen" to SelectFileCommand::class,
    "argument" to SelectFileCommand::class,
    "fir" to SelectFirstFileCommand::class,
    "firs" to SelectFirstFileCommand::class,
    "first" to SelectFirstFileCommand::class,
    "la" to SelectLastFileCommand::class,
    "las" to SelectLastFileCommand::class,
    "last" to SelectLastFileCommand::class,
    "se" to SetCommand::class,
    "set" to SetCommand::class,
    "setg" to SetglobalCommand::class,
    "setgl" to SetglobalCommand::class,
    "setglo" to SetglobalCommand::class,
    "setglob" to SetglobalCommand::class,
    "setgloba" to SetglobalCommand::class,
    "setglobal" to SetglobalCommand::class,
    "setl" to SetlocalCommand::class,
    "setlo" to SetlocalCommand::class,
    "setloc" to SetlocalCommand::class,
    "setloca" to SetlocalCommand::class,
    "setlocal" to SetlocalCommand::class,
    "sethandler" to SetHandlerCommand::class,
    "sh" to ShellCommand::class,
    "she" to ShellCommand::class,
    "shel" to ShellCommand::class,
    "shell" to ShellCommand::class,
    "sor" to SortCommand::class,
    "sort" to SortCommand::class,
    "sp" to SplitCommand::class,
    "spl" to SplitCommand::class,
    "spli" to SplitCommand::class,
    "split" to SplitCommand::class,
    "vs" to SplitCommand::class,
    "vsp" to SplitCommand::class,
    "vspl" to SplitCommand::class,
    "vspli" to SplitCommand::class,
    "vsplit" to SplitCommand::class,
    "so" to SourceCommand::class,
    "sou" to SourceCommand::class,
    "sour" to SourceCommand::class,
    "sourc" to SourceCommand::class,
    "source" to SourceCommand::class,
    "~" to SubstituteCommand::class,
    "&" to SubstituteCommand::class,
    "s" to SubstituteCommand::class,
    "su" to SubstituteCommand::class,
    "sub" to SubstituteCommand::class,
    "subs" to SubstituteCommand::class,
    "subst" to SubstituteCommand::class,
    "substi" to SubstituteCommand::class,
    "substit" to SubstituteCommand::class,
    "substitu" to SubstituteCommand::class,
    "substitut" to SubstituteCommand::class,
    "substitute" to SubstituteCommand::class,
    "tabc" to TabCloseCommand::class,
    "tabcl" to TabCloseCommand::class,
    "tabclo" to TabCloseCommand::class,
    "tabclos" to TabCloseCommand::class,
    "tabclose" to TabCloseCommand::class,
    "tabm" to TabMoveCommand::class,
    "tabmo" to TabMoveCommand::class,
    "tabmov" to TabMoveCommand::class,
    "tabmove" to TabMoveCommand::class,
    "tabo" to TabOnlyCommand::class,
    "tabon" to TabOnlyCommand::class,
    "tabonl" to TabOnlyCommand::class,
    "tabonly" to TabOnlyCommand::class,
    "u" to UndoCommand::class,
    "un" to UndoCommand::class,
    "und" to UndoCommand::class,
    "undo" to UndoCommand::class,
    "wa" to WriteAllCommand::class,
    "wal" to WriteAllCommand::class,
    "wall" to WriteAllCommand::class,
    "w" to WriteCommand::class,
    "wr" to WriteCommand::class,
    "wri" to WriteCommand::class,
    "writ" to WriteCommand::class,
    "write" to WriteCommand::class,
    "wn" to WriteNextFileCommand::class,
    "wne" to WriteNextFileCommand::class,
    "wnex" to WriteNextFileCommand::class,
    "wnext" to WriteNextFileCommand::class,
    "wN" to WritePreviousFileCommand::class,
    "wNe" to WritePreviousFileCommand::class,
    "wNex" to WritePreviousFileCommand::class,
    "wNext" to WritePreviousFileCommand::class,
    "wp" to WritePreviousFileCommand::class,
    "wpr" to WritePreviousFileCommand::class,
    "wpre" to WritePreviousFileCommand::class,
    "wprev" to WritePreviousFileCommand::class,
    "wprevi" to WritePreviousFileCommand::class,
    "wprevio" to WritePreviousFileCommand::class,
    "wpreviou" to WritePreviousFileCommand::class,
    "wprevious" to WritePreviousFileCommand::class,
    "wq" to WriteQuitCommand::class,
    "x" to WriteQuitCommand::class,
    "xi" to WriteQuitCommand::class,
    "xit" to WriteQuitCommand::class,
    "exi" to WriteQuitCommand::class,
    "exit" to WriteQuitCommand::class,
    "y" to YankLinesCommand::class,
    "ya" to YankLinesCommand::class,
    "yan" to YankLinesCommand::class,
    "yank" to YankLinesCommand::class,
    "map" to MapCommand::class,
    "nm" to MapCommand::class,
    "vm" to MapCommand::class,
    "xm" to MapCommand::class,
    "om" to MapCommand::class,
    "im" to MapCommand::class,
    "cm" to MapCommand::class,
    "nma" to MapCommand::class,
    "vma" to MapCommand::class,
    "xma" to MapCommand::class,
    "oma" to MapCommand::class,
    "ima" to MapCommand::class,
    "cma" to MapCommand::class,
    "nmap" to MapCommand::class,
    "vmap" to MapCommand::class,
    "xmap" to MapCommand::class,
    "omap" to MapCommand::class,
    "imap" to MapCommand::class,
    "cmap" to MapCommand::class,
    "no" to MapCommand::class,
    "nn" to MapCommand::class,
    "vn" to MapCommand::class,
    "xn" to MapCommand::class,
    "ono" to MapCommand::class,
    "ino" to MapCommand::class,
    "cno" to MapCommand::class,
    "nno" to MapCommand::class,
    "vno" to MapCommand::class,
    "xno" to MapCommand::class,
    "nor" to MapCommand::class,
    "nnor" to MapCommand::class,
    "vnor" to MapCommand::class,
    "xnor" to MapCommand::class,
    "onor" to MapCommand::class,
    "inor" to MapCommand::class,
    "cnor" to MapCommand::class,
    "nore" to MapCommand::class,
    "nnore" to MapCommand::class,
    "vnore" to MapCommand::class,
    "xnore" to MapCommand::class,
    "onore" to MapCommand::class,
    "inore" to MapCommand::class,
    "cnore" to MapCommand::class,
    "norem" to MapCommand::class,
    "nnorem" to MapCommand::class,
    "vnorem" to MapCommand::class,
    "xnorem" to MapCommand::class,
    "onorem" to MapCommand::class,
    "inorem" to MapCommand::class,
    "cnorem" to MapCommand::class,
    "norema" to MapCommand::class,
    "nnorema" to MapCommand::class,
    "vnorema" to MapCommand::class,
    "xnorema" to MapCommand::class,
    "onorema" to MapCommand::class,
    "inorema" to MapCommand::class,
    "cnorema" to MapCommand::class,
    "noremap" to MapCommand::class,
    "nnoremap" to MapCommand::class,
    "vnoremap" to MapCommand::class,
    "xnoremap" to MapCommand::class,
    "onoremap" to MapCommand::class,
    "inoremap" to MapCommand::class,
    "cnoremap" to MapCommand::class,
    "mapc" to MapClearCommand::class,
    "nmapc" to MapClearCommand::class,
    "vmapc" to MapClearCommand::class,
    "xmapc" to MapClearCommand::class,
    "smapc" to MapClearCommand::class,
    "omapc" to MapClearCommand::class,
    "imapc" to MapClearCommand::class,
    "cmapc" to MapClearCommand::class,
    "mapcl" to MapClearCommand::class,
    "nmapcl" to MapClearCommand::class,
    "vmapcl" to MapClearCommand::class,
    "xmapcl" to MapClearCommand::class,
    "smapcl" to MapClearCommand::class,
    "omapcl" to MapClearCommand::class,
    "imapcl" to MapClearCommand::class,
    "cmapcl" to MapClearCommand::class,
    "mapcle" to MapClearCommand::class,
    "nmapcle" to MapClearCommand::class,
    "vmapcle" to MapClearCommand::class,
    "xmapcle" to MapClearCommand::class,
    "smapcle" to MapClearCommand::class,
    "omapcle" to MapClearCommand::class,
    "imapcle" to MapClearCommand::class,
    "cmapcle" to MapClearCommand::class,
    "mapclea" to MapClearCommand::class,
    "nmapclea" to MapClearCommand::class,
    "vmapclea" to MapClearCommand::class,
    "xmapclea" to MapClearCommand::class,
    "smapclea" to MapClearCommand::class,
    "omapclea" to MapClearCommand::class,
    "imapclea" to MapClearCommand::class,
    "cmapclea" to MapClearCommand::class,
    "mapclear" to MapClearCommand::class,
    "nmapclear" to MapClearCommand::class,
    "vmapclear" to MapClearCommand::class,
    "xmapclear" to MapClearCommand::class,
    "smapclear" to MapClearCommand::class,
    "omapclear" to MapClearCommand::class,
    "imapclear" to MapClearCommand::class,
    "cmapclear" to MapClearCommand::class,
    "vu" to UnMapCommand::class,
    "xu" to UnMapCommand::class,
    "ou" to UnMapCommand::class,
    "iu" to UnMapCommand::class,
    "cu" to UnMapCommand::class,
    "nun" to UnMapCommand::class,
    "vun" to UnMapCommand::class,
    "xun" to UnMapCommand::class,
    "oun" to UnMapCommand::class,
    "iun" to UnMapCommand::class,
    "cun" to UnMapCommand::class,
    "unm" to UnMapCommand::class,
    "nunm" to UnMapCommand::class,
    "vunm" to UnMapCommand::class,
    "xunm" to UnMapCommand::class,
    "sunm" to UnMapCommand::class,
    "ounm" to UnMapCommand::class,
    "iunm" to UnMapCommand::class,
    "cunm" to UnMapCommand::class,
    "unma" to UnMapCommand::class,
    "nunma" to UnMapCommand::class,
    "vunma" to UnMapCommand::class,
    "xunma" to UnMapCommand::class,
    "sunma" to UnMapCommand::class,
    "ounma" to UnMapCommand::class,
    "iunma" to UnMapCommand::class,
    "cunma" to UnMapCommand::class,
    "unmap" to UnMapCommand::class,
    "nunmap" to UnMapCommand::class,
    "vunmap" to UnMapCommand::class,
    "xunmap" to UnMapCommand::class,
    "sunmap" to UnMapCommand::class,
    "ounmap" to UnMapCommand::class,
    "iunmap" to UnMapCommand::class,
    "cunmap" to UnMapCommand::class,
    "lockv" to LockVarCommand::class,
    "lockva" to LockVarCommand::class,
    "lockvar" to LockVarCommand::class,
    "unlo" to UnlockVarCommand::class,
    "unloc" to UnlockVarCommand::class,
    "unlock" to UnlockVarCommand::class,
    "unlockv" to UnlockVarCommand::class,
    "unlockva" to UnlockVarCommand::class,
    "unlockvar" to UnlockVarCommand::class,
  )

  private fun getCommandByName(commandName: String): KClass<out Command> {
    return if (injector.globalIjOptions().exCommandAnnotation) {
      injector.vimscriptParser.exCommands.getCommand(commandName)?.getKClass() ?: UnknownCommand::class
    } else {
      commands[commandName]!!
    }
  }
}
