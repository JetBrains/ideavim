/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
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

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.helper.MessageHelper
import com.maddyhome.idea.vim.helper.commandState

/**
 * @author vlan
 */
class OperatorAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_SELF_SYNCHRONIZED

  override val argumentType: Argument.Type = Argument.Type.MOTION

  override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
    val operatorFunction = VimPlugin.getKey().operatorFunction
    if (operatorFunction != null) {
      val argument = cmd.argument
      if (argument != null) {
        if (!editor.commandState.isDotRepeatInProgress) {
          VimRepeater.Extension.argumentCaptured = argument
        }
        val motion = argument.motion
        val range = MotionGroup
          .getMotionRange(editor, editor.caretModel.primaryCaret, context, cmd.count, cmd.rawCount, argument)
        if (range != null) {
          VimPlugin.getMark().setChangeMarks(editor, range)
          val selectionType = SelectionType.fromCommandFlags(motion.flags)
          KeyHandler.getInstance().reset(editor)
          return operatorFunction.apply(editor, context, selectionType)
        }
      }
      return false
    }
    VimPlugin.showMessage(MessageHelper.message("E774"))
    return false
  }
}
