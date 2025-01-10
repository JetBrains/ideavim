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
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.helper.EngineStringHelper
import com.maddyhome.idea.vim.mark.Mark
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

/**
 * see "h :marks"
 */
@ExCommand(command = "marks")
data class MarksCommand(val range: Range, val modifier: CommandModifier, val argument: String) :
  Command.SingleExecution(range, modifier, argument) {

  override val argFlags: CommandHandlerFlags =
    flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    val localMarks = injector.markService.getAllLocalMarks(editor.primaryCaret())
    val globalMarks = injector.markService.getAllGlobalMarks()

    // Yeah, lower case. Vim uses lower case here, but Title Case in :registers. Go figure.
    val res = (localMarks + globalMarks)
      .filter { argument.isEmpty() || argument.contains(it.key) }
      .sortedWith(Mark.KeySorter)
      .joinToString("\n", prefix = "mark line  col file/text\n") { mark ->

        // Lines are 1 based, columns zero based. See :help :marks
        val line = (mark.line + 1).toString().padStart(5)
        val column = mark.col.toString().padStart(3)
        val vf = editor.getVirtualFile()
        val text = if (vf != null && vf.path == mark.filepath) {
          val lineText = editor.getLineText(mark.line).trim().take(200)
          EngineStringHelper.toPrintableCharacters(injector.parser.stringToKeys(lineText)).take(200)
        } else {
          mark.filepath
        }

        " ${mark.key}  $line  $column $text"
      }

    injector.outputPanel.output(editor, context, res)

    return ExecutionResult.Success
  }
}
