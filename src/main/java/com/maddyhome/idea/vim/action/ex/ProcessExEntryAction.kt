/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.ex

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.action.ComplicatedKeysAction
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.VimActionHandler
import java.util.*
import javax.swing.KeyStroke

/**
 * Called by KeyHandler to process the contents of the ex entry panel
 *
 * The mapping for this action means that the ex command is executed as a write action
 */
class ProcessExEntryAction : VimActionHandler.SingleExecution(), ComplicatedKeysAction {
  override val keyStrokesSet: Set<List<KeyStroke>> =
    parseKeysSet("<CR>", "<C-M>", 0x0a.toChar().toString(), 0x0d.toChar().toString())

  override val type: Command.Type = Command.Type.OTHER_SELF_SYNCHRONIZED

  override val flags: EnumSet<CommandFlags> = EnumSet.of(CommandFlags.FLAG_COMPLETE_EX)

  override fun execute(editor: VimEditor, context: ExecutionContext, cmd: Command, operatorArguments: OperatorArguments): Boolean {
    return VimPlugin.getProcess().processExEntry(editor, context)
  }
}
