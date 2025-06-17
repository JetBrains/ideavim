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
import com.maddyhome.idea.vim.vimscript.model.CommandLineVimLContext
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.services.VimRcService
import java.io.File

/**
 * @author vlan
 * see "h :source"
 */
@ExCommand(command = "so[urce]")
data class SourceCommand(val range: Range, val modifier: CommandModifier, val argument: String) :
  Command.SingleExecution(range, modifier, argument) {

  override val argFlags: CommandHandlerFlags =
    flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_REQUIRED, Access.READ_ONLY)

  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    val path = argument.trim().vimExpanded()
    val file = File(path)
    injector.vimscriptExecutor.executeFile(
      file,
      editor,
      VimRcService.isIdeaVimRcFile(file),
      vimContext.getFirstParentContext() is CommandLineVimLContext
    )

    injector.statisticsService.addSourcedFile(path)
    return ExecutionResult.Success
  }

  private fun String.vimExpanded(): String {
    var expanded = this
    if (startsWith("~")) {
      val home = System.getProperty("user.home")
      if (home != null) {
        expanded = home + substring(1)
      }
    }

    val envRe = Regex("\\$[A-Za-z0-9_]+")
    val env = System.getenv()
    expanded = expanded.replace(envRe) { match ->
      val name = match.value.trimStart('$')
      val ret = env[name] ?: match.value
      ret
    }

    return expanded
  }
}
