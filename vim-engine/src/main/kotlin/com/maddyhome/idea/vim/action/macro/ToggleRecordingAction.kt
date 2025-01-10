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
import com.maddyhome.idea.vim.handler.VimActionHandler

@CommandOrMotion(keys = ["q"], modes = [Mode.NORMAL, Mode.VISUAL])
class ToggleRecordingAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_READONLY

  override val argumentType: Argument.Type?
    get() = if (!injector.registerGroup.isRecording) Argument.Type.CHARACTER else null

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    return if (!injector.registerGroup.isRecording) {
      val argument = cmd.argument as? Argument.Character ?: return false
      val reg = argument.character
      injector.registerGroup.startRecording(reg)
    } else {
      injector.registerGroup.finishRecording(editor, context)
      true
    }
  }
}
