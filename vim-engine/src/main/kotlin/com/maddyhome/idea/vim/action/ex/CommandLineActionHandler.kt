/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.ex

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCommandLine
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.VimActionHandler

abstract class CommandLineActionHandler : VimActionHandler.SingleExecution() {
  // Typically, handlers only need to change the command line text, which needs no synchronisation
  override val type: Command.Type = Command.Type.OTHER_SELF_SYNCHRONIZED

  final override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments
  ): Boolean {
    val commandLine = injector.commandLine.getActiveCommandLine() ?: return false
    // Make sure the prompt character is cleared (and view updated) before the command can update the command line text
    commandLine.clearPromptCharacter()
    return execute(commandLine, editor, context, cmd.argument)
  }

  protected open fun execute(
    commandLine: VimCommandLine,
    editor: VimEditor,
    context: ExecutionContext,
    argument: Argument?,
  ): Boolean {
    return execute(commandLine)
  }

  protected abstract fun execute(commandLine: VimCommandLine): Boolean
}
