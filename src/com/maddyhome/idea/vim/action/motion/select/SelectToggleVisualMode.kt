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

package com.maddyhome.idea.vim.action.motion.select

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.group.visual.updateCaretState
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.helper.commandState

/**
 * @author Alex Plate
 */

class SelectToggleVisualMode : VimActionHandler.SingleExecution() {

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
    val commandState = editor.commandState
    val subMode = commandState.subMode
    val mode = commandState.mode
    commandState.popState()
    if (mode == CommandState.Mode.VISUAL) {
      commandState.pushState(CommandState.Mode.SELECT, subMode, MappingMode.SELECT)
      if (subMode != CommandState.SubMode.VISUAL_LINE) {
        editor.caretModel.runForEachCaret {
          if (it.offset + VimPlugin.getVisualMotion().selectionAdj == it.selectionEnd) {
            it.moveToOffset(it.offset + VimPlugin.getVisualMotion().selectionAdj)
          }
        }
      }
    } else {
      commandState.pushState(CommandState.Mode.VISUAL, subMode, MappingMode.VISUAL)
      if (subMode != CommandState.SubMode.VISUAL_LINE) {
        editor.caretModel.runForEachCaret {
          if (it.offset == it.selectionEnd && it.visualLineStart <= it.offset - VimPlugin.getVisualMotion().selectionAdj) {
            it.moveToOffset(it.offset - VimPlugin.getVisualMotion().selectionAdj)
          }
        }
      }
    }
    updateCaretState(editor)
    return true
  }
}
