/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.helper.EngineStringHelper
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

/**
 * see "h :marks"
 */
data class MarksCommand(val ranges: Ranges, val argument: String) : Command.SingleExecution(ranges, argument) {
  override val argFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  override fun processCommand(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments): ExecutionResult {

    // Yeah, lower case. Vim uses lower case here, but Title Case in :registers. Go figure.
    val res = injector.markGroup.getMarks(editor)
      .filter { argument.isEmpty() || argument.contains(it.key) }
      .joinToString("\n", prefix = "mark line  col file/text\n") { mark ->

        // Lines are 1 based, columns zero based. See :help :marks
        val line = (mark.line + 1).toString().padStart(5)
        val column = mark.col.toString().padStart(3)
        val vf = editor.getVirtualFile()
        val text = if (vf != null && vf.path == mark.filename) {
          val lineText = editor.getLineText(mark.line).trim().take(200)
          EngineStringHelper.toPrintableCharacters(injector.parser.stringToKeys(lineText)).take(200)
        } else {
          mark.filename
        }

        " ${mark.key}  $line  $column $text"
      }

    injector.exOutputPanel.getPanel(editor).output(res)

    return ExecutionResult.Success
  }
}
