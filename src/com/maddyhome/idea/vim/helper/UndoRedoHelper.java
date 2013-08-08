/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2013 The IdeaVim authors
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.helper;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * @author oleg
 */
public class UndoRedoHelper {

  public static boolean undo(@NotNull final DataContext context) {
    final Project project = PlatformDataKeys.PROJECT.getData(context);
    final FileEditor fileEditor = PlatformDataKeys.FILE_EDITOR.getData(context);
    final com.intellij.openapi.command.undo.UndoManager undoManager = com.intellij.openapi.command.undo.UndoManager.getInstance(project);
    if (fileEditor != null && undoManager.isUndoAvailable(fileEditor)) {
      undoManager.undo(fileEditor);
      return true;
    }
    return false;
  }

  public static boolean redo(@NotNull final DataContext context) {
    final Project project = PlatformDataKeys.PROJECT.getData(context);
    final FileEditor fileEditor = PlatformDataKeys.FILE_EDITOR.getData(context);
    final com.intellij.openapi.command.undo.UndoManager undoManager = com.intellij.openapi.command.undo.UndoManager.getInstance(project);
    if (fileEditor != null && undoManager.isRedoAvailable(fileEditor)) {
      undoManager.redo(fileEditor);
      return true;
    }
    return false;
  }
}
