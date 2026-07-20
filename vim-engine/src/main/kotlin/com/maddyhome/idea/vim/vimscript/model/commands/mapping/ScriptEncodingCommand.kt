/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands.mapping

import com.intellij.vim.annotations.ExCommand
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.commands.Command.SingleExecution
import com.maddyhome.idea.vim.vimscript.model.commands.CommandModifier

// It's no-op in IdeaVim we just need it to not fail in vim scripts for keymap
@ExCommand(command = "scripte[ncoding]")
data class ScriptEncodingCommand(
  val range: Range,
  val modifier: CommandModifier,
  val argument: String,
) : SingleExecution(range, modifier, argument) {
  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    return ExecutionResult.Success
  }


  override val argFlags: CommandHandlerFlags
    get() = flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)
}