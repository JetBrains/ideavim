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
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.ExOutputModel
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.MessageHelper
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

/**
 * see "h :!"
 */
data class CmdFilterCommand(val ranges: Ranges, val argument: String) : Command.SingleExecution(ranges) {
  override val argFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.SELF_SYNCHRONIZED)

  override fun processCommand(editor: VimEditor, context: ExecutionContext): ExecutionResult {
    logger.debug("execute")
    val command = buildString {
      var inBackslash = false
      argument.forEach { c ->
        when {
          !inBackslash && c == '!' -> {
            val last = VimPlugin.getProcess().lastCommand
            if (last.isNullOrEmpty()) {
              VimPlugin.showMessage(MessageHelper.message("e_noprev"))
              return ExecutionResult.Error
            }
            append(last)
          }
          !inBackslash && c == '%' -> {
            val virtualFile = EditorHelper.getVirtualFile(editor.ij)
            if (virtualFile == null) {
              // Note that we use a slightly different error message to Vim, because we don't support alternate files or file
              // name modifiers. (I also don't know what the :p:h means)
              // (Vim) E499: Empty file name for '%' or '#', only works with ":p:h"
              // (IdeaVim) E499: Empty file name for '%'
              VimPlugin.showMessage(MessageHelper.message("E499"))
              return ExecutionResult.Error
            }
            append(virtualFile.path)
          }
          else -> append(c)
        }

        inBackslash = c == '\\'
      }
    }

    if (command.isEmpty()) {
      return ExecutionResult.Error
    }

    val workingDirectory = editor.ij.project?.basePath
    return try {
      if (ranges.size() == 0) {
        // Show command output in a window
        VimPlugin.getProcess().executeCommand(editor, command, null, workingDirectory)?.let {
          ExOutputModel.getInstance(editor.ij).output(it)
        }
        ExecutionResult.Success
      } else {
        // Filter
        val range = this.getTextRange(editor, false)
        val input = editor.ij.document.charsSequence.subSequence(range.startOffset, range.endOffset)
        VimPlugin.getProcess().executeCommand(editor, command, input, workingDirectory)?.let {
          ApplicationManager.getApplication().runWriteAction {
            val start = editor.offsetToLogicalPosition(range.startOffset)
            val end = editor.offsetToLogicalPosition(range.endOffset)
            editor.ij.document.replaceString(range.startOffset, range.endOffset, it)
            val linesFiltered = end.line - start.line
            if (linesFiltered > 2) {
              VimPlugin.showMessage("$linesFiltered lines filtered")
            }
          }
        }
        ExecutionResult.Success
      }
    } catch (e: ProcessCanceledException) {
      throw ExException("Command terminated")
    } catch (e: Exception) {
      throw ExException(e.message)
    }
  }

  companion object {
    private val logger = Logger.getInstance(CmdFilterCommand::class.java.name)
  }
}
