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

package com.maddyhome.idea.vim.helper

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.command.undo.UndoManager
import com.maddyhome.idea.vim.listener.SelectionVimListenerSuppressor

/**
 * @author oleg
 */
object UndoRedoHelper {
  fun undo(context: DataContext): Boolean {
    val project = PlatformDataKeys.PROJECT.getData(context) ?: return false
    val fileEditor = PlatformDataKeys.FILE_EDITOR.getData(context)
    val undoManager = UndoManager.getInstance(project)
    if (fileEditor != null && undoManager.isUndoAvailable(fileEditor)) {
      SelectionVimListenerSuppressor.lock().use { undoManager.undo(fileEditor) }
      return true
    }
    return false
  }

  fun redo(context: DataContext): Boolean {
    val project = PlatformDataKeys.PROJECT.getData(context) ?: return false
    val fileEditor = PlatformDataKeys.FILE_EDITOR.getData(context)
    val undoManager = UndoManager.getInstance(project)
    if (fileEditor != null && undoManager.isRedoAvailable(fileEditor)) {
      SelectionVimListenerSuppressor.lock().use { undoManager.redo(fileEditor) }
      return true
    }
    return false
  }
}
