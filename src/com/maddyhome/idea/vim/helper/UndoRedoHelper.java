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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.helper;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.maddyhome.idea.vim.group.SelectionVimListenerSuppressor;
import org.jetbrains.annotations.NotNull;

/**
 * @author oleg
 */
public class UndoRedoHelper {

  public static boolean undo(@NotNull final DataContext context) {
    final Project project = PlatformDataKeys.PROJECT.getData(context);
    final FileEditor fileEditor = PlatformDataKeys.FILE_EDITOR.getData(context);
    final Editor editor = PlatformDataKeys.EDITOR.getData(context);
    if (project == null) return false;
    final UndoManager undoManager = UndoManager.getInstance(project);
    if (editor != null && fileEditor != null && undoManager.isUndoAvailable(fileEditor)) {
      SelectionVimListenerSuppressor.INSTANCE.lock();
      undoManager.undo(fileEditor);
      // Visual mode should not be entered after undo. v_u will be called on next undo if visual mode stays
      editor.getSelectionModel().removeSelection(true);
      SelectionVimListenerSuppressor.INSTANCE.unlock();
      return true;
    }
    return false;
  }

  public static boolean redo(@NotNull final DataContext context) {
    final Project project = PlatformDataKeys.PROJECT.getData(context);
    if (project == null) return false;
    final FileEditor fileEditor = PlatformDataKeys.FILE_EDITOR.getData(context);
    final Editor editor = PlatformDataKeys.EDITOR.getData(context);
    final UndoManager undoManager = UndoManager.getInstance(project);
    if (editor != null && fileEditor != null && undoManager.isRedoAvailable(fileEditor)) {
      SelectionVimListenerSuppressor.INSTANCE.lock();
      undoManager.redo(fileEditor);
      // Visual mode should not be entered after redo. v_u will be called on next undo if visual mode stays
      editor.getSelectionModel().removeSelection(true);
      SelectionVimListenerSuppressor.INSTANCE.unlock();
      return true;
    }
    return false;
  }
}
