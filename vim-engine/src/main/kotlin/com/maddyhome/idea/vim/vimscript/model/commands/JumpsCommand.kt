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
import com.maddyhome.idea.vim.api.getJumpSpot
import com.maddyhome.idea.vim.api.getJumps
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.helper.EngineStringHelper
import com.maddyhome.idea.vim.mark.Jump
import com.maddyhome.idea.vim.mark.JumpSource
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import kotlin.math.absoluteValue

/**
 * see "h :jumps"
 */
@ExCommand(command = "ju[mps]", description = "Print the jump list, marking the current position with >.")
data class JumpsCommand(val range: Range, val modifier: CommandModifier, val argument: String) :
  Command.SingleExecution(range, modifier, argument) {

  override val argFlags: CommandHandlerFlags =
    flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_FORBIDDEN, Access.READ_ONLY)

  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    val jumps = injector.jumpService.getJumps(editor)
    val spot = injector.jumpService.getJumpSpot(editor)

    val text = StringBuilder(" jump line  col file/text\n")
    jumps.forEachIndexed { idx, jump ->
      val jumpSizeMinusSpot = jumps.size - idx - spot - 1
      text.append(if (jumpSizeMinusSpot == 0) ">" else " ")
      text.append(jumpSizeMinusSpot.absoluteValue.toString().padStart(3))
      text.append(" ")
      text.append((jump.line + 1).toString().padStart(5))

      text.append("  ")
      text.append(jump.col.toString().padStart(3))

      text.append(" ")
      text.append(placeOf(jump, editor))

      text.append("\n")
    }

    if (spot == -1) {
      text.append(">\n")
    }

    injector.outputPanel.clear(editor, context)
    injector.outputPanel.output(editor, context, text.toString())
    return ExecutionResult.Success
  }

  /** The text of the line of [jump] when [editor] shows it, otherwise the file of the jump */
  private fun placeOf(jump: Jump, editor: VimEditor): String {
    val source = jump.source
    return when {
      // This editor may not have the text of the line at all, e.g. when the jump is on the left side of a diff
      source is JumpSource.Diff -> "${jump.filepath} (diff, ${source.side.name.lowercase()})"
      editor.getVirtualFile()?.path == jump.filepath -> lineText(editor, jump.line)
      else -> jump.filepath
    }
  }

  private fun lineText(editor: VimEditor, line: Int): String {
    val keys = injector.parser.stringToKeys(editor.getLineText(line).trim().take(200))
    return EngineStringHelper.toPrintableCharacters(keys).take(200)
  }
}
