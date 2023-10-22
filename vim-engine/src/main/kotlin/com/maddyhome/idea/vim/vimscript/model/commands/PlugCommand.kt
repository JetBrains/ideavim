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
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

/**
 * This handler is created to support `Plug` command from vim-plug and `Plugin` command from vundle.
 */
@ExCommand(command = "Plug[in]")
public data class PlugCommand(val ranges: Ranges, val argument: String) : Command.SingleExecution(ranges, argument) {
  override val argFlags: CommandHandlerFlags = flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_REQUIRED, Access.READ_ONLY)

  override fun processCommand(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments): ExecutionResult {
    val argument = argument
    val firstChar = argument[0]
    if (firstChar != '"' && firstChar != '\'') return ExecutionResult.Error

    val pluginAlias = argument.drop(1).takeWhile { it != firstChar }
    if (!injector.extensionRegistrator.setOptionByPluginAlias(pluginAlias)) {
      return ExecutionResult.Error
    }

    injector.statisticsService.addExtensionEnabledWithPlug(injector.extensionRegistrator.getExtensionNameByAlias(pluginAlias)
      ?: "unknown extension")
    return ExecutionResult.Success
  }
}
