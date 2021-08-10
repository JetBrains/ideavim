package com.maddyhome.idea.vim.vimscript.parser.visitors

import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.ex.ranges.Range.Companion.createRange
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.vimscript.model.commands.ActionCommand
import com.maddyhome.idea.vim.vimscript.model.commands.ActionListCommand
import com.maddyhome.idea.vim.vimscript.model.commands.AsciiCommand
import com.maddyhome.idea.vim.vimscript.model.commands.BufferCloseCommand
import com.maddyhome.idea.vim.vimscript.model.commands.BufferCommand
import com.maddyhome.idea.vim.vimscript.model.commands.BufferListCommand
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
import com.maddyhome.idea.vim.vimscript.model.commands.DumpLineCommand
import com.maddyhome.idea.vim.vimscript.model.commands.EchoCommand
import com.maddyhome.idea.vim.vimscript.model.commands.EditFileCommand
import com.maddyhome.idea.vim.vimscript.model.commands.ExecuteCommand
import com.maddyhome.idea.vim.vimscript.model.commands.ExitCommand
import com.maddyhome.idea.vim.vimscript.model.commands.FileCommand
import com.maddyhome.idea.vim.vimscript.model.commands.FindClassCommand
import com.maddyhome.idea.vim.vimscript.model.commands.FindFileCommand
import com.maddyhome.idea.vim.vimscript.model.commands.FindSymbolCommand
import com.maddyhome.idea.vim.vimscript.model.commands.GlobalCommand
import com.maddyhome.idea.vim.vimscript.model.commands.GoToLineCommand
import com.maddyhome.idea.vim.vimscript.model.commands.GotoCharacterCommand
import com.maddyhome.idea.vim.vimscript.model.commands.HelpCommand
import com.maddyhome.idea.vim.vimscript.model.commands.HistoryCommand
import com.maddyhome.idea.vim.vimscript.model.commands.JoinLinesCommand
import com.maddyhome.idea.vim.vimscript.model.commands.JumpsCommand
import com.maddyhome.idea.vim.vimscript.model.commands.LetCommand
import com.maddyhome.idea.vim.vimscript.model.commands.MarkCommand
import com.maddyhome.idea.vim.vimscript.model.commands.MarksCommand
import com.maddyhome.idea.vim.vimscript.model.commands.MoveTextCommand
import com.maddyhome.idea.vim.vimscript.model.commands.NextFileCommand
import com.maddyhome.idea.vim.vimscript.model.commands.NextTabCommand
import com.maddyhome.idea.vim.vimscript.model.commands.NoHLSearchCommand
import com.maddyhome.idea.vim.vimscript.model.commands.OnlyCommand
import com.maddyhome.idea.vim.vimscript.model.commands.PlugCommand
import com.maddyhome.idea.vim.vimscript.model.commands.PreviousFileCommand
import com.maddyhome.idea.vim.vimscript.model.commands.PreviousTabCommand
import com.maddyhome.idea.vim.vimscript.model.commands.PrintCommand
import com.maddyhome.idea.vim.vimscript.model.commands.PromptFindCommand
import com.maddyhome.idea.vim.vimscript.model.commands.PromptReplaceCommand
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
import com.maddyhome.idea.vim.vimscript.model.commands.ShellCommand
import com.maddyhome.idea.vim.vimscript.model.commands.ShiftLeftCommand
import com.maddyhome.idea.vim.vimscript.model.commands.ShiftRightCommand
import com.maddyhome.idea.vim.vimscript.model.commands.SortCommand
import com.maddyhome.idea.vim.vimscript.model.commands.SourceCommand
import com.maddyhome.idea.vim.vimscript.model.commands.SplitCommand
import com.maddyhome.idea.vim.vimscript.model.commands.SplitType
import com.maddyhome.idea.vim.vimscript.model.commands.SubstituteCommand
import com.maddyhome.idea.vim.vimscript.model.commands.TabCloseCommand
import com.maddyhome.idea.vim.vimscript.model.commands.TabOnlyCommand
import com.maddyhome.idea.vim.vimscript.model.commands.UndoCommand
import com.maddyhome.idea.vim.vimscript.model.commands.UnknownCommand
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
import java.util.stream.Collectors

object CommandVisitor : VimscriptBaseVisitor<Command>() {

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
            throw ExException("Could not create range")
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
          throw ExException("Could not create range")
        }
      }
      if (ctx.children[ctx.childCount - 1] is VimscriptParser.RangeSeparatorContext) {
        val range = createRange(".", 0, false)
        if (range != null) {
          ranges.addRange(range)
        } else {
          throw ExException("Could not create range")
        }
        ranges.addRange(range)
      }
    }
    return ranges
  }

  override fun visitLetCommand(ctx: LetCommandContext): Command {
    val ranges: Ranges = parseRanges(ctx.range())
    val variable: Expression = expressionVisitor.visit(ctx.expr(0))
    val operator = getByValue(ctx.assignmentOperator.text)
    val expression: Expression = expressionVisitor.visit(ctx.expr(1))
    return LetCommand(ranges, variable, operator, expression)
  }

  override fun visitEchoCommand(ctx: EchoCommandContext): Command {
    val ranges: Ranges = parseRanges(ctx.range())
    val expressions = ctx.expr().stream()
      .map { tree: ExprContext ->
        expressionVisitor.visit(tree)
      }
      .collect(Collectors.toList())
    return EchoCommand(ranges, expressions)
  }

  override fun visitDelfunctionCommand(ctx: DelfunctionCommandContext): DelfunctionCommand {
    val ranges: Ranges = parseRanges(ctx.range())
    val functionScope =
      if (ctx.functionScope() != null) Scope.getByValue(ctx.functionScope().text) else null
    val functionName = ctx.functionName().text
    val ignoreIfMissing = ctx.replace != null
    return DelfunctionCommand(ranges, functionScope, functionName, ignoreIfMissing)
  }

  override fun visitGoToLineCommand(ctx: VimscriptParser.GoToLineCommandContext): Command {
    val ranges: Ranges = parseRanges(ctx.range())
    return GoToLineCommand(ranges)
  }

  override fun visitActionCommand(ctx: VimscriptParser.ActionCommandContext): ActionCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return ActionCommand(ranges, argument)
  }

  override fun visitActionListCommand(ctx: VimscriptParser.ActionListCommandContext): ActionListCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return ActionListCommand(ranges, argument)
  }

  override fun visitAsciiCommand(ctx: VimscriptParser.AsciiCommandContext): AsciiCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return AsciiCommand(ranges, argument)
  }

  override fun visitBufferCommand(ctx: VimscriptParser.BufferCommandContext): BufferCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return BufferCommand(ranges, argument)
  }

  override fun visitBufferCloseCommand(ctx: VimscriptParser.BufferCloseCommandContext): BufferCloseCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return BufferCloseCommand(ranges, argument)
  }

  override fun visitBufferListCommand(ctx: VimscriptParser.BufferListCommandContext): BufferListCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return BufferListCommand(ranges, argument)
  }

  override fun visitCmdCommand(ctx: VimscriptParser.CmdCommandContext): CmdCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return CmdCommand(ranges, argument)
  }

  override fun visitCmdClearCommand(ctx: VimscriptParser.CmdClearCommandContext): CmdClearCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return CmdClearCommand(ranges, argument)
  }

  override fun visitCmdFilterCommand(ctx: VimscriptParser.CmdFilterCommandContext): CmdFilterCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return CmdFilterCommand(ranges, argument)
  }

  override fun visitCopyTextCommand(ctx: VimscriptParser.CopyTextCommandContext): CopyTextCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return CopyTextCommand(ranges, argument)
  }

  override fun visitDelCmdCommand(ctx: VimscriptParser.DelCmdCommandContext): DelCmdCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return DelCmdCommand(ranges, argument)
  }

  override fun visitDeleteLinesCommand(ctx: VimscriptParser.DeleteLinesCommandContext): DeleteLinesCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return DeleteLinesCommand(ranges, argument)
  }

  override fun visitDeleteMarksCommand(ctx: VimscriptParser.DeleteMarksCommandContext): DeleteMarksCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return DeleteMarksCommand(ranges, argument)
  }

  override fun visitDigraphCommand(ctx: VimscriptParser.DigraphCommandContext): DigraphCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return DigraphCommand(ranges, argument)
  }

  override fun visitDumpLineCommand(ctx: VimscriptParser.DumpLineCommandContext): DumpLineCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return DumpLineCommand(ranges, argument)
  }

  override fun visitEditFileCommand(ctx: VimscriptParser.EditFileCommandContext): EditFileCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return EditFileCommand(ranges, argument)
  }

  override fun visitExitCommand(ctx: VimscriptParser.ExitCommandContext): ExitCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return ExitCommand(ranges, argument)
  }

  override fun visitFindFileCommand(ctx: VimscriptParser.FindFileCommandContext): FindFileCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return FindFileCommand(ranges, argument)
  }

  override fun visitFindClassCommand(ctx: VimscriptParser.FindClassCommandContext): FindClassCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return FindClassCommand(ranges, argument)
  }

  override fun visitFileCommand(ctx: VimscriptParser.FileCommandContext): FileCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return FileCommand(ranges, argument)
  }

  override fun visitFindSymbolCommand(ctx: VimscriptParser.FindSymbolCommandContext): FindSymbolCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return FindSymbolCommand(ranges, argument)
  }

  override fun visitGlobalCommand(ctx: VimscriptParser.GlobalCommandContext): GlobalCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    val invert = ctx.invert != null
    return GlobalCommand(ranges, argument, invert)
  }

  override fun visitVglobalCommand(ctx: VimscriptParser.VglobalCommandContext): GlobalCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return GlobalCommand(ranges, argument, true)
  }

  override fun visitGoToCharacterCommand(ctx: VimscriptParser.GoToCharacterCommandContext): GotoCharacterCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return GotoCharacterCommand(ranges, argument)
  }

  override fun visitHelpCommand(ctx: VimscriptParser.HelpCommandContext): HelpCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return HelpCommand(ranges, argument)
  }

  override fun visitHistoryCommand(ctx: VimscriptParser.HistoryCommandContext): HistoryCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return HistoryCommand(ranges, argument)
  }

  override fun visitJoinLinesCommand(ctx: VimscriptParser.JoinLinesCommandContext): JoinLinesCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return JoinLinesCommand(ranges, argument)
  }

  override fun visitJumpsCommand(ctx: VimscriptParser.JumpsCommandContext): JumpsCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return JumpsCommand(ranges, argument)
  }

  override fun visitMarkCommand(ctx: VimscriptParser.MarkCommandContext): MarkCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return MarkCommand(ranges, argument)
  }

  override fun visitMarksCommand(ctx: VimscriptParser.MarksCommandContext): MarksCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return MarksCommand(ranges, argument)
  }

  override fun visitMoveTextCommand(ctx: VimscriptParser.MoveTextCommandContext): MoveTextCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return MoveTextCommand(ranges, argument)
  }

  override fun visitNextFileCommand(ctx: VimscriptParser.NextFileCommandContext): NextFileCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return NextFileCommand(ranges, argument)
  }

  override fun visitNextTabCommand(ctx: VimscriptParser.NextTabCommandContext): NextTabCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return NextTabCommand(ranges, argument)
  }

  override fun visitNoHlSearchCommand(ctx: VimscriptParser.NoHlSearchCommandContext): NoHLSearchCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return NoHLSearchCommand(ranges, argument)
  }

  override fun visitOnlyCommand(ctx: VimscriptParser.OnlyCommandContext): OnlyCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return OnlyCommand(ranges, argument)
  }

  override fun visitPlugCommand(ctx: VimscriptParser.PlugCommandContext): PlugCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return PlugCommand(ranges, argument)
  }

  override fun visitPreviousFileCommand(ctx: VimscriptParser.PreviousFileCommandContext): PreviousFileCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return PreviousFileCommand(ranges, argument)
  }

  override fun visitPreviousTabCommand(ctx: VimscriptParser.PreviousTabCommandContext): PreviousTabCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return PreviousTabCommand(ranges, argument)
  }

  override fun visitPrintCommand(ctx: VimscriptParser.PrintCommandContext): PrintCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return PrintCommand(ranges, argument)
  }

  override fun visitPromptFindCommand(ctx: VimscriptParser.PromptFindCommandContext): PromptFindCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return PromptFindCommand(ranges, argument)
  }

  override fun visitPromptReplaceCommand(ctx: VimscriptParser.PromptReplaceCommandContext): PromptReplaceCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return PromptReplaceCommand(ranges, argument)
  }

  override fun visitPutLinesCommand(ctx: VimscriptParser.PutLinesCommandContext): PutLinesCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return PutLinesCommand(ranges, argument)
  }

  override fun visitQuitCommand(ctx: VimscriptParser.QuitCommandContext): QuitCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return QuitCommand(ranges, argument)
  }

  override fun visitRedoCommand(ctx: VimscriptParser.RedoCommandContext): RedoCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return RedoCommand(ranges, argument)
  }

  override fun visitRegistersCommand(ctx: VimscriptParser.RegistersCommandContext): RegistersCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return RegistersCommand(ranges, argument)
  }

  override fun visitRepeatCommand(ctx: VimscriptParser.RepeatCommandContext): RepeatCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return RepeatCommand(ranges, argument)
  }

  override fun visitSelectFileCommand(ctx: VimscriptParser.SelectFileCommandContext): SelectFileCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return SelectFileCommand(ranges, argument)
  }

  override fun visitSelectFirstFileCommand(ctx: VimscriptParser.SelectFirstFileCommandContext): SelectFirstFileCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return SelectFirstFileCommand(ranges, argument)
  }

  override fun visitSelectLastFileCommand(ctx: VimscriptParser.SelectLastFileCommandContext): SelectLastFileCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return SelectLastFileCommand(ranges, argument)
  }

  override fun visitSetCommand(ctx: VimscriptParser.SetCommandContext): SetCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return SetCommand(ranges, argument)
  }

  override fun visitSetHandlerCommand(ctx: VimscriptParser.SetHandlerCommandContext): SetHandlerCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return SetHandlerCommand(ranges, argument)
  }

  override fun visitShellCommand(ctx: VimscriptParser.ShellCommandContext): ShellCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return ShellCommand(ranges, argument)
  }

  override fun visitShiftLeftCommand(ctx: VimscriptParser.ShiftLeftCommandContext): ShiftLeftCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    val length = ctx.lShift().text.length
    return ShiftLeftCommand(ranges, argument, length)
  }

  override fun visitShiftRightCommand(ctx: VimscriptParser.ShiftRightCommandContext): ShiftRightCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    val length = ctx.rShift().text.length
    return ShiftRightCommand(ranges, argument, length)
  }

  override fun visitSortCommand(ctx: VimscriptParser.SortCommandContext): SortCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return SortCommand(ranges, argument)
  }

  override fun visitSplitCommand(ctx: VimscriptParser.SplitCommandContext): SplitCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return SplitCommand(ranges, argument, SplitType.HORIZONTAL)
  }

  override fun visitVSplitCommand(ctx: VimscriptParser.VSplitCommandContext): SplitCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return SplitCommand(ranges, argument, SplitType.VERTICAL)
  }

  override fun visitSourceCommand(ctx: VimscriptParser.SourceCommandContext): SourceCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return SourceCommand(ranges, argument)
  }

  override fun visitSubstituteCommand(ctx: VimscriptParser.SubstituteCommandContext): SubstituteCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return SubstituteCommand(ranges, argument, ctx.substituteCommandName.text)
  }

  override fun visitTabOnlyCommand(ctx: VimscriptParser.TabOnlyCommandContext): TabOnlyCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return TabOnlyCommand(ranges, argument)
  }

  override fun visitTabCloseCommand(ctx: VimscriptParser.TabCloseCommandContext): TabCloseCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return TabCloseCommand(ranges, argument)
  }

  override fun visitUndoCommand(ctx: VimscriptParser.UndoCommandContext): UndoCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return UndoCommand(ranges, argument)
  }

  override fun visitWriteAllCommand(ctx: VimscriptParser.WriteAllCommandContext): WriteAllCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return WriteAllCommand(ranges, argument)
  }

  override fun visitWriteCommand(ctx: VimscriptParser.WriteCommandContext): WriteCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return WriteCommand(ranges, argument)
  }

  override fun visitWriteNextCommand(ctx: VimscriptParser.WriteNextCommandContext): WriteNextFileCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return WriteNextFileCommand(ranges, argument)
  }

  override fun visitWritePreviousCommand(ctx: VimscriptParser.WritePreviousCommandContext): WritePreviousFileCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return WritePreviousFileCommand(ranges, argument)
  }

  override fun visitWriteQuitCommand(ctx: VimscriptParser.WriteQuitCommandContext): WriteQuitCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return WriteQuitCommand(ranges, argument)
  }

  override fun visitYankLinesCommand(ctx: VimscriptParser.YankLinesCommandContext): YankLinesCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text.trim()
    return YankLinesCommand(ranges, argument)
  }

  override fun visitMapCommand(ctx: VimscriptParser.MapCommandContext): MapCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text
    val cmd = ctx.MAP().text
    return MapCommand(ranges, argument, cmd)
  }

  override fun visitUnmapCommand(ctx: VimscriptParser.UnmapCommandContext): UnMapCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text
    val cmd = ctx.UNMAP().text
    return UnMapCommand(ranges, argument, cmd)
  }

  override fun visitMapClearCommand(ctx: VimscriptParser.MapClearCommandContext): MapClearCommand {
    val ranges = parseRanges(ctx.range())
    val argument = ctx.commandArgument().text
    val cmd = ctx.MAP_CLEAR().text
    return MapClearCommand(ranges, argument, cmd)
  }

  override fun visitExecuteCommand(ctx: VimscriptParser.ExecuteCommandContext): ExecuteCommand {
    val ranges = parseRanges(ctx.range())
    val expressions = ctx.expr().stream()
      .map { tree: ExprContext ->
        expressionVisitor.visit(tree)
      }
      .collect(Collectors.toList())
    return ExecuteCommand(ranges, expressions)
  }

  override fun visitOtherCommand(ctx: OtherCommandContext): UnknownCommand {
    val ranges: Ranges = parseRanges(ctx.range())
    val name = ctx.commandName().text
    val argument = ctx.commandArgument()?.text ?: ""
    return UnknownCommand(ranges, name, argument)
  }
}
