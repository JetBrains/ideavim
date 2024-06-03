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
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.helper.enumSetOf
import com.maddyhome.idea.vim.helper.vimStateMachine
import com.maddyhome.idea.vim.state.mode.ReturnableFromCmd
import java.util.*

@CommandOrMotion(keys = [":"], modes = [Mode.NORMAL, Mode.VISUAL, Mode.OP_PENDING])
internal class ExEntryAction : VimActionHandler.SingleExecution() {
  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_START_EX)
  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(editor: VimEditor, context: ExecutionContext, cmd: Command, operatorArguments: OperatorArguments): Boolean {
    if (editor.isOneLineMode()) return false
    injector.processGroup.startExEntry(editor, context, cmd)
    return true
  }
}
