/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands

import com.intellij.vim.annotations.ExCommand
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.options
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.history.VimHistory
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

/**
 * see "h :history"
 */
@ExCommand(command = "his[tory]")
data class HistoryCommand(val range: Range, val modifier: CommandModifier, val argument: String) :
  Command.SingleExecution(range, modifier, argument) {

  override val argFlags: CommandHandlerFlags =
    flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    if (modifier == CommandModifier.BANG) {
      throw exExceptionMessage("E477")  // E477: No ! allowed
    }

    if (injector.options(editor).history == 0) {
      injector.messages.showStatusBarMessage(editor, injector.messages.message("message.history.option.is.zero"))
      return ExecutionResult.Success
    }

    // Strip any trailing whitespace. There won't be leading whitespace
    val arg = argument.trim()

    // The argument is an optional type, followed by optional whitespace, followed by an optional range. First, let's
    // figure out what the type is, and get the offset of the next part to parse
    val (type, offset) = parseType(arg)

    var first: Int
    var last: Int

    // The rest is either empty, a from value, a from/to pair, or trailing characters
    val rest = arg.substring(offset)
    val regex = """^\s*((?<first>-?\d+)(\s*,\s*(?<last>-?\d+))?)(?<trailing>.*)""".toRegex()
    val match = regex.matchEntire(rest)
    if (match == null && rest.isNotEmpty() && rest.isNotBlank()) {
      throw exExceptionMessage("E488", rest.trim())
    }
    else {
      val trailing = match?.groups?.get("trailing")?.value ?: ""
      if (trailing.isNotBlank()) {
        throw exExceptionMessage("E488", trailing.trim())  // E488: Trailing characters: $trailing
      }

      first = match?.groups?.get("first")?.value?.toIntOrNull() ?: 0
      last = match?.groups?.get("last")?.value?.toIntOrNull() ?: 0
    }

    val text = buildString {
      // Order is important here
      if (type == CMD || type == ALL) {
        outputHistory(CMD, VimHistory.Type.Command, first, last)
      }
      if (type == ALL) appendLine()
      if (type == SEARCH || type == ALL) {
        outputHistory(SEARCH, VimHistory.Type.Search, first, last)
      }
      if (type == ALL) appendLine()
      if (type == EXPRESSION || type == ALL) {
        outputHistory(EXPRESSION, VimHistory.Type.Expression, first, last)
      }
      if (type == ALL) appendLine()
      if (type == INPUT || type == ALL) {
        outputHistory(INPUT, VimHistory.Type.Input, first, last)
      }
      if (type == ALL) appendLine()
    }

    injector.outputPanel.output(editor, context, text)
    return ExecutionResult.Success
  }

  private fun parseType(arg: String): Pair<String, Int> {
    if (arg.isEmpty() || arg[0].isDigit()) return Pair(CMD, 0)

    when (arg[0]) {
      ':' -> return Pair(CMD, 1)
      '/', '?' -> return Pair(SEARCH, 1)
      '=' -> return Pair(EXPRESSION, 1)
      '@' -> return Pair(INPUT, 1)
    }

    return parseTypePrefix(arg, CMD)
      ?: parseTypePrefix(arg, SEARCH)
      ?: parseTypePrefix(arg, EXPRESSION)
      ?: parseTypePrefix(arg, INPUT)
      ?: parseTypePrefix(arg, ALL)
      ?: throw exExceptionMessage("E488", arg)  // E488: Trailing characters: $arg
  }

  private fun parseTypePrefix(arg: String, type: String): Pair<String, Int>? {
    val prefix = arg.commonPrefixWith(type)
    if (prefix.isNotEmpty()) {
      return Pair(type, prefix.length)
    }
    return null
  }

  private fun StringBuilder.outputHistory(name: String, type: VimHistory.Type, start: Int, end: Int) {
    append("      #  $name history")
    injector.historyGroup.getEntries(type, start, end).forEach {
      val num = it.number.toString().padStart(7)
      appendLine()
      append("$num  ${it.entry}")
    }
  }

  companion object {
    private const val CMD = "cmd"
    private const val SEARCH = "search"
    private const val EXPRESSION = "expr"
    private const val INPUT = "input"
    private const val ALL = "all"
  }
}
