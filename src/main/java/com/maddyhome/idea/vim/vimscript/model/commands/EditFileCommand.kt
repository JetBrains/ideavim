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

package com.maddyhome.idea.vim.vimscript.model.commands

import com.intellij.openapi.application.ApplicationManager
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.helper.EditorDataContext
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

/**
 * see "h :edit"
 */
data class EditFileCommand(val ranges: Ranges, val argument: String) : Command.SingleExecution(ranges, argument) {
  override val argFlags = flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)
  override fun processCommand(editor: VimEditor, context: ExecutionContext): ExecutionResult {
    val arg = argument
    if (arg == "#") {
      VimPlugin.getMark().saveJumpLocation(editor)
      VimPlugin.getFile().selectPreviousTab(context.ij)
      return ExecutionResult.Success
    } else if (arg.isNotEmpty()) {
      val res = VimPlugin.getFile().openFile(arg, context.ij)
      if (res) {
        VimPlugin.getMark().saveJumpLocation(editor)
      }
      return if (res) ExecutionResult.Success else ExecutionResult.Error
    }

    // Don't open a choose file dialog under a write action
    ApplicationManager.getApplication().invokeLater {
      injector.actionExecutor.executeAction("OpenFile", EditorDataContext.init(editor.ij, context.ij).vim)
    }

    return ExecutionResult.Success
  }
}
