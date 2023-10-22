/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorComposite;
import com.intellij.openapi.fileEditor.impl.EditorTabbedContainer;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.concurrency.annotations.RequiresReadLock;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.api.ExecutionContext;
import com.maddyhome.idea.vim.api.VimCaret;
import com.maddyhome.idea.vim.helper.MessageHelper;
import com.maddyhome.idea.vim.helper.RWLockLabel;
import com.maddyhome.idea.vim.newapi.IjEditorExecutionContext;
import com.maddyhome.idea.vim.newapi.IjVimCaret;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

public class WindowGroup extends WindowGroupBase {
  @Override
  public void closeCurrentWindow(@NotNull ExecutionContext context) {
    final FileEditorManagerEx fileEditorManager = getFileEditorManager((DataContext) context.getContext());
    final EditorWindow window = fileEditorManager.getSplitters().getCurrentWindow();
    if (window != null) {
      window.closeAllExcept(null);
    }
  }

  @Override
  public void closeAllExceptCurrent(@NotNull ExecutionContext context) {
    final FileEditorManagerEx fileEditorManager = getFileEditorManager(((DataContext) context.getContext()));
    final EditorWindow current = fileEditorManager.getCurrentWindow();
    for (final EditorWindow window : fileEditorManager.getWindows()) {
      if (window != current) {
        window.closeAllExcept(null);
      }
    }
  }

  public void closeAll(@NotNull ExecutionContext context) {
    getFileEditorManager(((IjEditorExecutionContext) context).getContext()).closeAllFiles();
  }

  @Override
  public void selectNextWindow(@NotNull ExecutionContext context) {
    final FileEditorManagerEx fileEditorManager = getFileEditorManager(((DataContext) context.getContext()));
    final EditorWindow current = fileEditorManager.getCurrentWindow();
    if (current != null) {
      EditorWindow nextWindow = fileEditorManager.getNextWindow(current);
      if (nextWindow != null) {
        nextWindow.setAsCurrentWindow(true);
      }
    }
  }

  @Override
  public void selectPreviousWindow(@NotNull ExecutionContext context) {
    final FileEditorManagerEx fileEditorManager = getFileEditorManager(((DataContext) context.getContext()));
    final EditorWindow current = fileEditorManager.getCurrentWindow();
    if (current != null) {
      EditorWindow prevWindow = fileEditorManager.getPrevWindow(current);
      if (prevWindow != null) {
        prevWindow.setAsCurrentWindow(true);
      }
    }
  }

  @Override
  public void selectWindow(@NotNull ExecutionContext context, int index) {
    final FileEditorManagerEx fileEditorManager = getFileEditorManager(((DataContext) context.getContext()));
    final EditorWindow[] windows = fileEditorManager.getWindows();
    if (index - 1 < windows.length) {
      windows[index - 1].setAsCurrentWindow(true);
    }
  }

  @Override
  public void splitWindowHorizontal(@NotNull ExecutionContext context, @NotNull String filename) {
    splitWindow(SwingConstants.HORIZONTAL, (DataContext) context.getContext(), filename);
  }

  @Override
  public void splitWindowVertical(@NotNull ExecutionContext context, @NotNull String filename) {
    splitWindow(SwingConstants.VERTICAL, (DataContext) context.getContext(), filename);
  }

  @Override
  @RWLockLabel.Readonly
  @RequiresReadLock
  public void selectWindowInRow(@NotNull VimCaret caret, @NotNull ExecutionContext context, int relativePosition, boolean vertical) {
    final Caret ijCaret = ((IjVimCaret) caret).getCaret();
    final FileEditorManagerEx fileEditorManager = getFileEditorManager(((DataContext) context.getContext()));
    final EditorWindow currentWindow = fileEditorManager.getCurrentWindow();
    if (currentWindow != null) {
      final EditorWindow[] windows = fileEditorManager.getWindows();
      final List<EditorWindow> row = findWindowsInRow(ijCaret, currentWindow, Arrays.asList(windows), vertical);
      selectWindow(currentWindow, row, relativePosition);
    }
  }

  private void selectWindow(@NotNull EditorWindow currentWindow, @NotNull List<EditorWindow> windows,
                            int relativePosition) {
    final int pos = windows.indexOf(currentWindow);
    final int selected = pos + relativePosition;
    final int normalized = Math.max(0, Math.min(selected, windows.size() - 1));
    windows.get(normalized).setAsCurrentWindow(true);
  }

  private static @NotNull
  List<EditorWindow> findWindowsInRow(@NotNull Caret caret,
                                      @NotNull EditorWindow editorWindow,
                                      @NotNull List<EditorWindow> windows, final boolean vertical) {
    final Point anchorPoint = getCaretPoint(caret);
    if (anchorPoint != null) {
      final List<EditorWindow> result = new ArrayList<>();
      final double coord = vertical ? anchorPoint.getX() : anchorPoint.getY();
      for (EditorWindow window : windows) {
        final Rectangle rect = getSplitRectangle(window);
        if (rect != null) {
          final double min = vertical ? rect.getX() : rect.getY();
          final double max = min + (vertical ? rect.getWidth() : rect.getHeight());
          if (coord >= min && coord <= max) {
            result.add(window);
          }
        }
      }
      result.sort((window1, window2) -> {
        final Rectangle rect1 = getSplitRectangle(window1);
        final Rectangle rect2 = getSplitRectangle(window2);
        if (rect1 != null && rect2 != null) {
          final double diff = vertical ? (rect1.getY() - rect2.getY()) : (rect1.getX() - rect2.getX());
          return diff < 0 ? -1 : diff > 0 ? 1 : 0;
        }
        return 0;
      });
      return result;
    }
    return Collections.singletonList(editorWindow);
  }

  private static @NotNull
  FileEditorManagerEx getFileEditorManager(@NotNull DataContext context) {
    final Project project = PlatformDataKeys.PROJECT.getData(context);
    return FileEditorManagerEx.getInstanceEx(Objects.requireNonNull(project));
  }

  private void splitWindow(int orientation, @NotNull DataContext context, @NotNull String filename) {
    final Project project = PlatformDataKeys.PROJECT.getData(context);
    if (project == null) return;
    final FileEditorManagerEx fileEditorManager = FileEditorManagerEx.getInstanceEx(project);

    VirtualFile virtualFile = null;
    if (filename.length() > 0) {
      virtualFile = VimPlugin.getFile().findFile(filename, project);
      if (virtualFile == null) {
        VimPlugin.showMessage(MessageHelper.message("could.not.find.file.0", filename));
        return;
      }
    }

    final EditorWindow editorWindow = fileEditorManager.getSplitters().getCurrentWindow();
    if (editorWindow != null) {
      editorWindow.split(orientation, true, virtualFile, true);
    }
  }

  private static @NotNull
  Point getCaretPoint(@NotNull Caret caret) {
    final Editor editor = caret.getEditor();
    final Point caretLocation = editor.logicalPositionToXY(caret.getLogicalPosition());
    Point caretScreenLocation = editor.getContentComponent().getLocationOnScreen();
    caretScreenLocation.translate(caretLocation.x, caretLocation.y);
    return caretScreenLocation;
  }

  private static @Nullable
  Rectangle getSplitRectangle(@NotNull EditorWindow window) {
    final EditorComposite editorComposite = window.getSelectedComposite();
    if (editorComposite != null) {
      final EditorTabbedContainer split = window.getTabbedPane();
      final Point point = split.getComponent().getLocationOnScreen();
      final Dimension dimension = split.getComponent().getSize();
      return new Rectangle(point, dimension);
    }
    return null;
  }
}
