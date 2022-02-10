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

package com.maddyhome.idea.vim.action.motion.visual

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.group.visual.vimSetSelection
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.helper.vimForEachCaret
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.services.OptionConstants
import com.maddyhome.idea.vim.vimscript.services.OptionService

class VisualToggleLineModeAction : VimActionHandler.SingleExecution() {

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(editor: Editor, context: DataContext, cmd: Command, operatorArguments: OperatorArguments): Boolean {
    val listOption = (VimPlugin.getOptionService().getOptionValue(OptionService.Scope.LOCAL(IjVimEditor(editor)), OptionConstants.selectmodeName) as VimString).value
    return if ("cmd" in listOption) {
      VimPlugin.getVisualMotion().enterSelectMode(editor, CommandState.SubMode.VISUAL_LINE).also {
        editor.vimForEachCaret { it.vimSetSelection(it.offset) }
      }
    } else VimPlugin.getVisualMotion()
      .toggleVisual(editor, cmd.count, cmd.rawCount, CommandState.SubMode.VISUAL_LINE)
  }
}
