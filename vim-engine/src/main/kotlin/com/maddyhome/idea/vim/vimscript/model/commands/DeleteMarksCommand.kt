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
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.mark.VimMarkConstants
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

/**
 * @author Jørgen Granseth
 * see "h :delmarks"
 */

private val VIML_COMMENT = Regex("(?<!\\\\)\".*")
private val TRAILING_SPACES = Regex("\\s*$")

private const val ESCAPED_QUOTE = "\\\""
private const val UNESCAPED_QUOTE = "\""

@ExCommand(command = "delm[arks]")
data class DeleteMarksCommand(val range: Range, val modifier: CommandModifier, val argument: String) :
  Command.SingleExecution(range, modifier, argument) {

  override val argFlags: CommandHandlerFlags =
    flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    val processedArg = if (modifier == CommandModifier.BANG) {
      if (argument.isNotEmpty()) throw exExceptionMessage("E474")
      VimMarkConstants.DEL_FILE_MARKS
    } else {
      argument.replace(VIML_COMMENT, "")
        .replace(ESCAPED_QUOTE, UNESCAPED_QUOTE)
        .replace(TRAILING_SPACES, "")
        .replaceRanges(VimMarkConstants.WR_REGULAR_FILE_MARKS)
        .replaceRanges(VimMarkConstants.WR_GLOBAL_MARKS)
        .replaceRanges(VimMarkConstants.RO_GLOBAL_MARKS)
    }

    processedArg.indexOfFirst { it !in " ${VimMarkConstants.DEL_MARKS}" }.let { index ->
      if (index != -1) {
        val invalidIndex = if (processedArg[index] == '-') (index - 1).coerceAtLeast(0) else index

        injector.messages.showStatusBarMessage(
          editor,
          injector.messages.message("E475", processedArg.substring(invalidIndex))
        )
        return ExecutionResult.Error
      }
    }

    processedArg.forEach { character -> injector.markService.removeMark(editor, character) }

    return ExecutionResult.Success
  }
}

private fun String.replaceRanges(range: String): String {
  return Regex("[$range]-[$range]").replace(this) { match ->
    val startChar = match.value[0]
    val endChar = match.value[2]

    val startIndex = range.indexOf(startChar)
    val endIndex = range.indexOf(endChar)

    if (startIndex >= 0 && endIndex >= 0 && startIndex <= endIndex) {
      range.subSequence(startIndex, endIndex + 1)
    } else {
      match.value
    }
  }
}
