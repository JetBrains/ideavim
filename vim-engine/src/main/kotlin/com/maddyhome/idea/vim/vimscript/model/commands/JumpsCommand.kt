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
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.helper.EngineStringHelper
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import kotlin.math.absoluteValue

/**
 * see "h :jumps"
 */
data class JumpsCommand(val ranges: Ranges, val argument: String) : Command.SingleExecution(ranges, argument) {
  override val argFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_FORBIDDEN, Access.READ_ONLY)
  override fun processCommand(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments): ExecutionResult {
    val jumps = injector.markGroup.getJumps()
    val spot = injector.markGroup.getJumpSpot()

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
      val vf = editor.getVirtualFile()
      if (vf != null && vf.path == jump.filepath) {
        val line = editor.getLineText(jump.line).trim().take(200)
        val keys = injector.parser.stringToKeys(line)
        text.append(EngineStringHelper.toPrintableCharacters(keys).take(200))
      } else {
        text.append(jump.filepath)
      }

      text.append("\n")
    }

    if (spot == -1) {
      text.append(">\n")
    }

    injector.exOutputPanel.getPanel(editor).output(text.toString())

    return ExecutionResult.Success
  }
}
