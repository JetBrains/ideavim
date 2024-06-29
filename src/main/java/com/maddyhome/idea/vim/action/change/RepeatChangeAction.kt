/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.change

import com.intellij.openapi.command.CommandProcessor
import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.newapi.ij

@CommandOrMotion(keys = ["."], modes = [Mode.NORMAL])
internal class RepeatChangeAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_WRITABLE

  override fun execute(editor: VimEditor, context: ExecutionContext, cmd: Command, operatorArguments: OperatorArguments): Boolean {
    val state = injector.vimState
    val lastCommand = VimRepeater.lastChangeCommand

    if (lastCommand == null && Extension.lastExtensionHandler == null) return false

    // Save state
    val save = state.executingCommand
    val lastFTCmd = injector.motion.lastFTCmd
    val lastFTChar = injector.motion.lastFTChar
    val reg = injector.registerGroup.currentRegister
    val lastHandler = Extension.lastExtensionHandler
    val repeatHandler = VimRepeater.repeatHandler

    state.isDotRepeatInProgress = true

    // A fancy 'redo-register' feature
    // VIM-2643, :h redo-register
    if (VimRepeater.lastChangeRegister in '1'..'8') {
      VimRepeater.lastChangeRegister = VimRepeater.lastChangeRegister.inc()
    }

    injector.registerGroup.selectRegister(VimRepeater.lastChangeRegister)

    if (repeatHandler && lastHandler != null) {
      val processor = CommandProcessor.getInstance()
      processor.executeCommand(
        editor.ij.project,
        { lastHandler.execute(editor, context, operatorArguments) },
        "Vim " + lastHandler.javaClass.simpleName,
        null,
      )
    } else if (!repeatHandler && lastCommand != null) {
      if (cmd.rawCount > 0) {
        lastCommand.rawCount = cmd.count
        val arg = lastCommand.argument
        if (arg != null) {
          val mot = arg.motion
          mot.rawCount = 0
        }
      }
      state.executingCommand = lastCommand

      val arguments = operatorArguments.copy(count0 = lastCommand.rawCount)
      injector.actionExecutor.executeVimAction(editor, lastCommand.action, context, arguments)

      VimRepeater.saveLastChange(lastCommand)
    }

    state.isDotRepeatInProgress = false

    // Restore state
    if (save != null) state.executingCommand = save
    VimPlugin.getMotion().setLastFTCmd(lastFTCmd, lastFTChar)
    if (lastHandler != null) Extension.lastExtensionHandler = lastHandler
    VimRepeater.repeatHandler = repeatHandler
    Extension.reset()
    VimPlugin.getRegister().selectRegister(reg)
    return true
  }
}
