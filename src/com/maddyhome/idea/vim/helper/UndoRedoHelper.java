package com.maddyhome.idea.vim.helper;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;

/**
 * @author oleg
 */
public class UndoRedoHelper {

  public static boolean undo(final DataContext context) {
    final Project project = PlatformDataKeys.PROJECT.getData(context);
    final FileEditor fileEditor = PlatformDataKeys.FILE_EDITOR.getData(context);
    final com.intellij.openapi.command.undo.UndoManager undoManager = com.intellij.openapi.command.undo.UndoManager.getInstance(project);
    if (fileEditor != null && undoManager.isUndoAvailable(fileEditor)) {
      undoManager.undo(fileEditor);
      return true;
    }
    return false;
  }

  public static boolean redo(final DataContext context) {
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
