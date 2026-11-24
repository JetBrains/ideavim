/*
 * Copyright 2003-2026 The IdeaVim authors
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
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

/**
 * see "h :new"
 */
@ExCommand(
  command = "new",
  description = "Split the current editor window horizontally and open a new scratch file in the new split. With [file], open it instead."
)
data class NewCommand(val range: Range, val modifier: CommandModifier, val argument: String) :
  NewWindowCommand(range, modifier, argument, SplitType.HORIZONTAL)

/**
 * see "h :vnew"
 */
@ExCommand(
  command = "vne[w]",
  description = "Split the current editor window vertically and open a new scratch file in the new split. With [file], open it instead."
)
data class VnewCommand(val range: Range, val modifier: CommandModifier, val argument: String) :
  NewWindowCommand(range, modifier, argument, SplitType.VERTICAL)

sealed class NewWindowCommand(
  range: Range,
  modifier: CommandModifier,
  argument: String,
  val splitType: SplitType,
) : Command.SingleExecution(range, modifier, argument) {

  override val argFlags: CommandHandlerFlags =
    flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    val path = if (commandArgument.isBlank()) {
      injector.file.createScratchFile(context) ?: return ExecutionResult.Error
    } else {
      injector.pathExpansion.expandPath(commandArgument, editor)
    }
    if (splitType == SplitType.VERTICAL) {
      injector.window.splitWindowVertical(context, path)
    } else {
      injector.window.splitWindowHorizontal(context, path)
    }
    return ExecutionResult.Success
  }
}
