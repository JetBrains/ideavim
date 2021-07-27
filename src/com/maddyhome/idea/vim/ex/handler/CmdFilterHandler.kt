/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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

package com.maddyhome.idea.vim.ex.handler

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProcessCanceledException
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.ex.CommandHandler
import com.maddyhome.idea.vim.ex.ExCommand
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.ExOutputModel
import com.maddyhome.idea.vim.ex.flags
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.MessageHelper

class CmdFilterHandler : CommandHandler.SingleExecution() {
  override val argFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.SELF_SYNCHRONIZED)

  override fun execute(editor: Editor, context: DataContext, cmd: ExCommand): Boolean {
    logger.debug("execute")

    val command = buildString {
      var inBackslash = false
      cmd.argument.forEach { c ->
        when {
          !inBackslash && c == '!' -> {
            val last = VimPlugin.getProcess().lastCommand
            if (last.isNullOrEmpty()) {
              VimPlugin.showMessage(MessageHelper.message("e_noprev"))
              return false
            }
            append(last)
          }
          !inBackslash && c == '%' -> {
            val virtualFile = EditorHelper.getVirtualFile(editor)
            if (virtualFile == null) {
              // Note that we use a slightly different error message to Vim, because we don't support alternate files or file
              // name modifiers. (I also don't know what the :p:h means)
              // (Vim) E499: Empty file name for '%' or '#', only works with ":p:h"
              // (IdeaVim) E499: Empty file name for '%'
              VimPlugin.showMessage(MessageHelper.message("E499"))
              return false
            }
            append(virtualFile.path)
          }
          else -> append(c)
        }

        inBackslash = c == '\\'
      }
    }

    if (command.isEmpty()) {
      return false
    }

    return try {
      if (cmd.ranges.size() == 0) {
        // Show command output in a window
        VimPlugin.getProcess().executeCommand(editor, command, null)?.let {
          ExOutputModel.getInstance(editor).output(it)
        }
        true
      } else {
        // Filter
        val range = cmd.getTextRange(editor, false)
        val input = editor.document.charsSequence.subSequence(range.startOffset, range.endOffset)
        VimPlugin.getProcess().executeCommand(editor, command, input)?.let {
          ApplicationManager.getApplication().runWriteAction {
            val start = editor.offsetToLogicalPosition(range.startOffset)
            val end = editor.offsetToLogicalPosition(range.endOffset)
            editor.document.replaceString(range.startOffset, range.endOffset, it)
            val linesFiltered = end.line - start.line
            if (linesFiltered > 2) {
              VimPlugin.showMessage("$linesFiltered lines filtered")
            }
          }
        }
        true
      }
    } catch (e: ProcessCanceledException) {
      throw ExException("Command terminated")
    } catch (e: Exception) {
      throw ExException(e.message)
    }
  }

  companion object {
    private val logger = Logger.getInstance(CmdFilterHandler::class.java.name)
  }
}
