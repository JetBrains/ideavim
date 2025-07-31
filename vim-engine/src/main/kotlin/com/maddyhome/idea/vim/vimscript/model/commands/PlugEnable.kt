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
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

/**
 * Command that is used for enabling plugins. E.g. `:PlugEnable [plugin-name]`
 */
@ExCommand(command = "PlugEnable")
class PlugEnable(val range: Range, val modifier: CommandModifier, val argument: String) :
  Command.SingleExecution(range, modifier, argument) {

  override val argFlags: CommandHandlerFlags =
    flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_REQUIRED, Access.READ_ONLY)

  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    val extensionName = argument
    if (extensionName.isEmpty()) return ExecutionResult.Error

    val extension = injector.jsonExtensionProvider.getExtension(extensionName) ?: return ExecutionResult.Error
    injector.extensionLoader.enableExtension(extension)

    return ExecutionResult.Success
  }
}