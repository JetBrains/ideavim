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

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.group.visual.vimSetSystemSelectionSilently
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.moveToInlayAwareOffset
import com.maddyhome.idea.vim.helper.vimLastColumn
import com.maddyhome.idea.vim.newapi.ij

/**
 * @author Alex Plate
 */

class SelectEnableBlockModeAction : VimActionHandler.SingleExecution() {

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(editor: VimEditor, context: ExecutionContext, cmd: Command, operatorArguments: OperatorArguments): Boolean {
    editor.ij.caretModel.removeSecondaryCarets()
    val lineEnd = EditorHelper.getLineEndForOffset(editor.ij, editor.ij.caretModel.primaryCaret.offset)
    editor.ij.caretModel.primaryCaret.run {
      vimSetSystemSelectionSilently(offset, (offset + 1).coerceAtMost(lineEnd))
      moveToInlayAwareOffset((offset + 1).coerceAtMost(lineEnd))
      vimLastColumn = visualPosition.column
    }
    return VimPlugin.getVisualMotion().enterSelectMode(editor, CommandState.SubMode.VISUAL_BLOCK)
  }
}
