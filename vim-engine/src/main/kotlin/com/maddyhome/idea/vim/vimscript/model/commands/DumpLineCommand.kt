/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getLineEndOffset
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

/**
 * see "h :dumpline"
 */
public data class DumpLineCommand(val ranges: Ranges, val argument: String) : Command.SingleExecution(ranges, argument) {
  override val argFlags: CommandHandlerFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)
  override fun processCommand(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments): ExecutionResult {
    if (!logger.isDebug()) return ExecutionResult.Error

    val range = getLineRange(editor)
    val chars = editor.text()
    for (l in range.startLine..range.endLine) {
      val start = editor.getLineStartOffset(l)
      val end = editor.getLineEndOffset(l, true)

      logger.debug("Line $l, start offset=$start, end offset=$end")

      for (i in start..end) {
        logger.debug(
          "Offset $i, char=${chars[i]}, lp=${editor.offsetToBufferPosition(i)}, vp=${editor.offsetToVisualPosition(i)}",
        )
      }
    }

    return ExecutionResult.Success
  }

  public companion object {
    private val logger = vimLogger<DumpLineCommand>()
  }
}
