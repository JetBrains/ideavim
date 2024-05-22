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
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.helper.enumSetOf
import com.maddyhome.idea.vim.state.mode.ReturnableFromCmd
import java.util.*

@CommandOrMotion(keys = ["/"], modes = [Mode.NORMAL, Mode.VISUAL, Mode.OP_PENDING])
public class SearchEntryFwdAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_SELF_SYNCHRONIZED

  override val argumentType: Argument.Type = Argument.Type.EX_STRING
  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_START_EX, CommandFlags.FLAG_SAVE_JUMP)

  override fun execute( editor: VimEditor, context: ExecutionContext, cmd: Command, operatorArguments: OperatorArguments): Boolean {
    injector.processGroup.startSearchCommand(editor, context, cmd.count, '/')
    val currentMode = editor.mode
    check(currentMode is ReturnableFromCmd) { "Cannot enable command line mode $currentMode" }
    editor.mode = com.maddyhome.idea.vim.state.mode.Mode.CMD_LINE(currentMode)
    return true
  }
}
