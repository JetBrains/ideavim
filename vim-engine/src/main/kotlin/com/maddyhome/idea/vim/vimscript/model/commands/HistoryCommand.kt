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
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.history.HistoryConstants.COMMAND
import com.maddyhome.idea.vim.history.HistoryConstants.EXPRESSION
import com.maddyhome.idea.vim.history.HistoryConstants.INPUT
import com.maddyhome.idea.vim.history.HistoryConstants.SEARCH
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
    logger.debug("execute")

    var arg = argument.trim().ifEmpty { "cmd" }

    var key: String
    val spos = arg.indexOf(' ')
    if (spos >= 0) {
      key = arg.take(spos).trim()
      arg = arg.substring(spos + 1)
    } else {
      key = arg
      arg = ""
    }

    logger.debug("key='$key'")

    if (key.length == 1 && key[0] in ":/=@") {
      when (key[0]) {
        ':' -> key = "cmd"
        '/' -> key = "search"
        '=' -> key = "expr"
        '@' -> key = "input"
      }
    } else if (key[0].isLetter()) {
      if (!"cmd".startsWith(key) &&
        !"search".startsWith(key) &&
        !"expr".startsWith(key) &&
        !"input".startsWith(key) &&
        !"all".startsWith(key)
      ) {
        // Invalid command
        logger.debug("invalid command $key")
        return ExecutionResult.Error
      }
    } else {
      arg = "$key $arg"
      key = "cmd"
    }

    val first: String
    val last: String
    val cpos = arg.indexOf(',')
    if (cpos >= 0) {
      first = arg.substring(0, cpos).trim()
      last = arg.substring(cpos + 1).trim()
    } else {
      first = arg
      last = ""
    }

    val f = if (first.isNotEmpty()) {
      first.toIntOrNull() ?: run {
        logger.debug("bad number")
        return ExecutionResult.Error
      }
    } else {
      0
    }
    val l = if (last.isNotEmpty()) {
      last.toIntOrNull() ?: run {
        logger.debug("bad number")
        return ExecutionResult.Error
      }
    } else {
      0
    }

    val p = processKey(f, l)
    val res = when (key[0]) {
      'c' -> p(COMMAND)
      's' -> p(SEARCH)
      'e' -> p(EXPRESSION)
      'i' -> p(INPUT)
      'a' -> "${p(COMMAND)}${p(SEARCH)}${p(EXPRESSION)}${p(INPUT)}"
      else -> ""
    }

    injector.outputPanel.output(editor, context, res)
    return ExecutionResult.Success
  }

  private fun processKey(start: Int, end: Int) = { key: String ->
    logger.debug("process $key $start,$end")

    injector.historyGroup.getEntries(key, start, end).joinToString("\n", prefix = "      #  $key history\n") { entry ->
      val num = entry.number.toString().padStart(7)
      "$num  ${entry.entry}"
    }
  }

  companion object {
    private val logger = vimLogger<HistoryCommand>()
  }
}
