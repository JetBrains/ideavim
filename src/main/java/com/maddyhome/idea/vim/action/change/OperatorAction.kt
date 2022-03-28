/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.maddyhome.idea.vim.action.change

import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.common.argumentCaptured
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.helper.MessageHelper
import com.maddyhome.idea.vim.helper.commandState
import com.maddyhome.idea.vim.newapi.ij

/**
 * @author vlan
 */
class OperatorAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_SELF_SYNCHRONIZED

  override val argumentType: Argument.Type = Argument.Type.MOTION

  override fun execute(editor: VimEditor, context: ExecutionContext, cmd: Command, operatorArguments: OperatorArguments): Boolean {
    val operatorFunction = VimPlugin.getKey().operatorFunction
    if (operatorFunction != null) {
      val argument = cmd.argument
      if (argument != null) {
        if (!editor.commandState.isDotRepeatInProgress) {
          argumentCaptured = argument
        }
        val saveRepeatHandler = VimRepeater.repeatHandler
        val motion = argument.motion
        val range = MotionGroup
          .getMotionRange(
            editor.ij,
            editor.ij.caretModel.primaryCaret,
            context.ij,
            argument,
            operatorArguments
          )
        if (range != null) {
          VimPlugin.getMark().setChangeMarks(editor, range)
          val selectionType = if (motion.isLinewiseMotion()) SelectionType.LINE_WISE else SelectionType.CHARACTER_WISE
          KeyHandler.getInstance().reset(editor)
          val result = operatorFunction.apply(editor.ij, context.ij, selectionType)
          VimRepeater.repeatHandler = saveRepeatHandler
          return result
        }
      }
      return false
    }
    VimPlugin.showMessage(MessageHelper.message("E774"))
    return false
  }
}
