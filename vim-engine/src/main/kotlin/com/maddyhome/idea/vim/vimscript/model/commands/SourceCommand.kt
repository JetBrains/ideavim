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
import com.maddyhome.idea.vim.vimscript.model.CommandLineVimLContext
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import java.io.File

/**
 * @author vlan
 * see "h :source"
 */
@ExCommand(command = "so[urce]")
public data class SourceCommand(val ranges: Ranges, val argument: String) : Command.SingleExecution(ranges, argument) {
  override val argFlags: CommandHandlerFlags = flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_REQUIRED, Access.READ_ONLY)
  override fun processCommand(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments): ExecutionResult {
    val path = expandUser(argument.trim())
    injector.vimscriptExecutor.executeFile(File(path), vimContext.getFirstParentContext() is CommandLineVimLContext)

    injector.statisticsService.addSourcedFile(path)
    return ExecutionResult.Success
  }

  private fun expandUser(path: String): String {
    if (path.startsWith("~")) {
      val home = System.getProperty("user.home")
      if (home != null) {
        return home + path.substring(1)
      }
    }
    return path
  }
}
