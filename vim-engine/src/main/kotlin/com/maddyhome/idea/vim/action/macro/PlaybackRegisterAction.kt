/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.macro

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.register.RegisterConstants.LAST_COMMAND_REGISTER

@CommandOrMotion(keys = ["@"], modes = [Mode.NORMAL])
class PlaybackRegisterAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_SELF_SYNCHRONIZED

  override val argumentType: Argument.Type = Argument.Type.CHARACTER

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val argument = cmd.argument as? Argument.Character ?: return false
    val reg = argument.character
    val res = arrayOf(false)
    when {
      reg == LAST_COMMAND_REGISTER || (reg == '@' && injector.macro.lastRegister == LAST_COMMAND_REGISTER) -> {
        try {
          var i = 0
          while (i < cmd.count) {
            res[0] = injector.vimscriptExecutor.executeLastCommand(editor, context)
            if (!res[0]) {
              break
            }
            i += 1
          }
          if (reg != '@') { // @ is not a register itself, it just tells vim to use the last register
            injector.macro.lastRegister = reg
          }
        } catch (_: ExException) {
          res[0] = false
        }
      }

      reg == '@' -> {
        res[0] = injector.macro.playbackLastRegister(editor, context, cmd.count)
      }

      else -> {
        res[0] = injector.macro.playbackRegister(editor, context, reg, cmd.count)
      }
    }
    return res[0]
  }
}
