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

package com.maddyhome.idea.vim.action.motion.select

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.helper.inVisualMode
import com.maddyhome.idea.vim.helper.pushSelectMode
import com.maddyhome.idea.vim.helper.pushVisualMode
import com.maddyhome.idea.vim.helper.vimStateMachine

/**
 * @author Alex Plate
 */

class SelectToggleVisualMode : VimActionHandler.SingleExecution() {

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    toggleMode(editor)
    return true
  }

  companion object {
    fun toggleMode(editor: VimEditor) {
      val commandState = editor.vimStateMachine
      val subMode = commandState.subMode
      val mode = commandState.mode
      commandState.popModes()
      if (mode.inVisualMode) {
        commandState.pushSelectMode(subMode, mode)
        if (subMode != VimStateMachine.SubMode.VISUAL_LINE) {
          editor.nativeCarets().forEach {
            if (it.offset.point + injector.visualMotionGroup.selectionAdj == it.selectionEnd) {
              it.moveToInlayAwareOffset(it.offset.point + injector.visualMotionGroup.selectionAdj)
            }
          }
        }
      } else {
        commandState.pushVisualMode(subMode, mode)
        if (subMode != VimStateMachine.SubMode.VISUAL_LINE) {
          editor.nativeCarets().forEach {
            if (it.offset.point == it.selectionEnd && it.visualLineStart <= it.offset.point - injector.visualMotionGroup.selectionAdj) {
              it.moveToInlayAwareOffset(it.offset.point - injector.visualMotionGroup.selectionAdj)
            }
          }
        }
      }
    }
  }
}
