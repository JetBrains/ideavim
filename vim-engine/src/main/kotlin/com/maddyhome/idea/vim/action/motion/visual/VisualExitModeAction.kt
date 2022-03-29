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

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.VimActionHandler

/**
 * @author vlan
 */
class VisualExitModeAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    editor.exitVisualModeNative()

    editor.forEachCaret { caret ->
      val lineEnd = editor.lineEndForOffset(caret.offset.point)
      if (lineEnd == caret.offset.point) {
        val position = injector.motion.getOffsetOfHorizontalMotion(editor, caret, -1, false)
        caret.moveToOffset(position)
      }
    }
    // Should it be in [exitVisualMode]?
    editor.forEachCaret { caret ->
      val lineEnd = injector.engineEditorHelper.getLineEndForOffset(editor, caret.offset.point)
      if (lineEnd == caret.offset.point) {
        val position = injector.motion.getOffsetOfHorizontalMotion(editor, caret, -1, false)
        caret.moveToOffset(position)
      }
    }

    return true
  }
}
