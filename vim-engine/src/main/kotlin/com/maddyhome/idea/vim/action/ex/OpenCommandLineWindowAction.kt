/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.ex

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.helper.enumSetOf
import java.util.*

/**
 * `c_CTRL-F` — leaves the command line and opens the command-line window with the typed text as its last line.
 *
 * Unlike [LeaveCommandLineAction], the typed text isn't added to history, it is still being edited in the cmdwin.
 */
@CommandOrMotion(
  keys = ["<C-F>"],
  modes = [Mode.CMD_LINE],
  description = "Open the command-line window to edit the command line being typed."
)
class OpenCommandLineWindowAction : VimActionHandler.SingleExecution() {
  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_END_EX)
  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val commandLine = cmd.argument as? Argument.ExString ?: return false
    injector.searchWindowGroup.openFromCommandLine(editor, context, commandLine)
    return true
  }
}
