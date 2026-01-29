/*
 * Copyright 2003-2025 The IdeaVim authors
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
import com.maddyhome.idea.vim.helper.EngineStringHelper
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

@ExCommand(command = "=")
data class PrintLineNumberCommand(val range: Range, val modifier: CommandModifier, val argument: String) :
  Command.SingleExecution(range, modifier, argument) {

  init {
    defaultRange = "$"
  }

  override val argFlags: CommandHandlerFlags =
    flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    if (argument.isNotEmpty() && argument[0] !in "l#p") {
      throw exExceptionMessage("E488", argument)
    }

    val line1 = range.getLineRange(editor, editor.currentCaret()).endLine1.coerceAtMost(editor.lineCount())

    // `l` means output the line like `:list` - show unprintable chars, and include `^` and `$`
    // `#` means output the line with the line number
    // `p` means output the line like `:print`
    // The flags can be combined, so `l#` means line number and `:list`. Normally, Vim displays this over two lines.
    // Since we're outputting to the single line status bar, if any flags are specified, we treat it like `#` was
    // specified - we always show line number.
    val content = if (argument.isNotEmpty()) {
      val text = editor.getLineText(line1 - 1)
      if (argument.contains("l")) {
        val keys = injector.parser.stringToKeys(text)
        EngineStringHelper.toPrintableCharacters(keys)
      }
      else text
    }
    else ""

    injector.messages.showErrorMessage(editor, "$line1 $content")
    return ExecutionResult.Success
  }
}
