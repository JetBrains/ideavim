/*
 * Copyright 2003-2024 The IdeaVim authors
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
import com.maddyhome.idea.vim.history.VimHistory
import java.util.*

@CommandOrMotion(keys = ["<Esc>", "<C-[>", "<C-C>"], modes = [Mode.CMD_LINE])
class LeaveCommandLineAction : VimActionHandler.SingleExecution() {
  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_END_EX)
  override val type: Command.Type = Command.Type.MODE_CHANGE

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val argument = cmd.argument as? Argument.ExString ?: return true
    val historyType = VimHistory.Type.getTypeByLabel(argument.label.toString())
    injector.historyGroup.addEntry(historyType, argument.string)
    return true
  }
}
