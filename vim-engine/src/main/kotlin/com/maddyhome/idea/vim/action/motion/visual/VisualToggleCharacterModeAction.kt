/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.motion.visual

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.options
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.state.mode.SelectionType

@CommandOrMotion(keys = ["v"], modes = [Mode.NORMAL, Mode.VISUAL])
class VisualToggleCharacterModeAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    return if (injector.options(editor).selectmode.contains(OptionConstants.selectmode_cmd)) {
      injector.visualMotionGroup.enterSelectMode(editor, SelectionType.CHARACTER_WISE)
    } else {
      injector.visualMotionGroup.toggleVisual(editor, cmd.count, cmd.rawCount, SelectionType.CHARACTER_WISE)
    }
  }
}
