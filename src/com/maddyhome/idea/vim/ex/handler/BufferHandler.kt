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
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.ex.CommandHandler
import com.maddyhome.idea.vim.ex.ExCommand
import com.maddyhome.idea.vim.ex.flags
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.MessageHelper

/**
 * Handles buffer, buf, bu, b.
 *
 * @author John Weigel
 */
class BufferHandler : CommandHandler.SingleExecution() {
  override val argFlags = flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  override fun execute(editor: Editor, context: DataContext, cmd: ExCommand): Boolean {
    val arg = cmd.argument.trim()
    val overrideModified = arg.startsWith('!')
    val buffer = if (overrideModified) arg.replace(Regex("^!\\s*"), "") else arg
    var result = true

    if (buffer.isNotEmpty()) {
      if (buffer.matches(Regex("^\\d+$"))) {
        val bufNum = buffer.toInt() - 1

        if (!VimPlugin.getFile().selectFile(bufNum, context)) {
          VimPlugin.showMessage(MessageHelper.message("buffer.0.does.not.exist", bufNum))
          result = false
        }
      } else {
        val editors = findPartialMatch(context, buffer)

        when (editors.size) {
          0 -> {
            VimPlugin.showMessage(MessageHelper.message("no.matching.buffer.for.0", buffer))
            result = false
          }
          1 -> {
            if (EditorHelper.hasUnsavedChanges(editor) && !overrideModified) {
              VimPlugin.showMessage(MessageHelper.message("no.write.since.last.change.add.to.override"))
              result = false
            } else {
              VimPlugin.getFile().openFile(EditorHelper.getVirtualFile(editors[0])!!.name, context)
            }
          }
          else -> {
            VimPlugin.showMessage(MessageHelper.message("more.than.one.match.for.0", buffer))
            result = false
          }
        }
      }
    }

    return result
  }

  private fun findPartialMatch(context: DataContext, fileName: String): List<Editor> {
    val matchedFiles = mutableListOf<Editor>()
    val project = PlatformDataKeys.PROJECT.getData(context) ?: return matchedFiles

    for (file in FileEditorManager.getInstance(project).openFiles) {
      if (file.name.contains(fileName)) {
        val editor = EditorHelper.getEditor(file) ?: continue
        matchedFiles.add(editor)
      }
    }

    return matchedFiles
  }
}

