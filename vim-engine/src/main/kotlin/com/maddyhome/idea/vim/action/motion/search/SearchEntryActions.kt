/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.motion.search

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.helper.enumSetOf
import java.util.*

@CommandOrMotion(keys = ["/"], modes = [Mode.NORMAL, Mode.VISUAL, Mode.OP_PENDING])
class SearchEntryFwdAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.MODE_CHANGE
  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_START_EX, CommandFlags.FLAG_SAVE_JUMP)

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    startSearchCommand('/', editor, context)
    return true
  }
}

@CommandOrMotion(keys = ["?"], modes = [Mode.NORMAL, Mode.VISUAL, Mode.OP_PENDING])
class SearchEntryRevAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.MODE_CHANGE
  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_START_EX, CommandFlags.FLAG_SAVE_JUMP)

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    startSearchCommand('?', editor, context)
    return true
  }
}

private fun startSearchCommand(label: Char, editor: VimEditor, context: ExecutionContext) {
  injector.commandLine.createSearchPrompt(editor, context, label.toString(), "")
}
