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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.group;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.fileEditor.impl.EditorWithProviderComposite;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.helper.RWLockLabel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class WindowGroup {
  public void closeCurrentWindow(@NotNull DataContext context) {
    final FileEditorManagerEx fileEditorManager = getFileEditorManager(context);
    final EditorWindow window = fileEditorManager.getSplitters().getCurrentWindow();
    if (window != null) {
      window.closeAllExcept(null);
    }
  }

  public void closeAllExceptCurrent(@NotNull DataContext context) {
    final FileEditorManagerEx fileEditorManager = getFileEditorManager(context);
    final EditorWindow current = fileEditorManager.getCurrentWindow();
    for (final EditorWindow window : fileEditorManager.getWindows()) {
      if (window != current) {
        window.closeAllExcept(null);
      }
    }
  }

  public void closeAllExceptCurrentTab(@NotNull DataContext context) {
    final EditorWindow currentWindow = getFileEditorManager(context).getCurrentWindow();
    currentWindow.closeAllExcept(currentWindow.getSelectedFile());
  }

  public void closeAll(@NotNull DataContext context) {
    getFileEditorManager(context).closeAllFiles();
  }

  public void selectNextWindow(@NotNull DataContext context) {
    final FileEditorManagerEx fileEditorManager = getFileEditorManager(context);
    final EditorWindow current = fileEditorManager.getCurrentWindow();
    if (current != null) {
      fileEditorManager.getNextWindow(current).setAsCurrentWindow(true);
    }
  }

  public void selectPreviousWindow(@NotNull DataContext context) {
    final FileEditorManagerEx fileEditorManager = getFileEditorManager(context);
    final EditorWindow current = fileEditorManager.getCurrentWindow();
    if (current != null) {
      fileEditorManager.getPrevWindow(current).setAsCurrentWindow(true);
    }
  }

  public void selectWindow(@NotNull DataContext context, int index) {
    final FileEditorManagerEx fileEditorManager = getFileEditorManager(context);
    final EditorWindow[] windows = fileEditorManager.getWindows();
    if (index - 1 < windows.length) {
      windows[index - 1].setAsCurrentWindow(true);
    }
  }

  public void splitWindowHorizontal(@NotNull DataContext context, @NotNull String filename) {
    splitWindow(SwingConstants.HORIZONTAL, context, filename);
  }

  public void splitWindowVertical(@NotNull DataContext context, @NotNull String filename) {
    splitWindow(SwingConstants.VERTICAL, context, filename);
  }

  @RWLockLabel.Readonly
  public void selectWindowInRow(@NotNull DataContext context, int relativePosition, boolean vertical) {
    final FileEditorManagerEx fileEditorManager = getFileEditorManager(context);
    final EditorWindow currentWindow = fileEditorManager.getCurrentWindow();
    if (currentWindow != null) {
      final EditorWindow[] windows = fileEditorManager.getWindows();
      final List<EditorWindow> row = findWindowsInRow(currentWindow, Arrays.asList(windows), vertical);
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

  @NotNull
  private static List<EditorWindow> findWindowsInRow(@NotNull EditorWindow anchor,
                                                     @NotNull List<EditorWindow> windows, final boolean vertical) {
    final Rectangle anchorRect = getEditorWindowRectangle(anchor);
    if (anchorRect != null) {
      final List<EditorWindow> result = new ArrayList<>();
      final double coord = vertical ? anchorRect.getX() : anchorRect.getY();
      for (EditorWindow window : windows) {
        final Rectangle rect = getEditorWindowRectangle(window);
        if (rect != null) {
          final double min = vertical ? rect.getX() : rect.getY();
          final double max = min + (vertical ? rect.getWidth() : rect.getHeight());
          if (coord >= min && coord <= max) {
            result.add(window);
          }
        }
      }
      result.sort((window1, window2) -> {
        final Rectangle rect1 = getEditorWindowRectangle(window1);
        final Rectangle rect2 = getEditorWindowRectangle(window2);
        if (rect1 != null && rect2 != null) {
          final double diff = vertical ? (rect1.getY() - rect2.getY()) : (rect1.getX() - rect2.getX());
          return diff < 0 ? -1 : diff > 0 ? 1 : 0;
        }
        return 0;
      });
      return result;
    }
    return Collections.singletonList(anchor);
  }

  @NotNull
  private static FileEditorManagerEx getFileEditorManager(@NotNull DataContext context) {
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
        VimPlugin.showMessage("Could not find file: " + filename);
        return;
      }
    }

    final EditorWindow editorWindow = fileEditorManager.getSplitters().getCurrentWindow();
    if (editorWindow != null) {
      editorWindow.split(orientation, true, virtualFile, true);
    }
  }

  @Nullable
  private static Rectangle getEditorWindowRectangle(@NotNull EditorWindow window) {
    final EditorWithProviderComposite editor = window.getSelectedEditor();
    if (editor != null) {
      final Point point = editor.getComponent().getLocationOnScreen();
      final Dimension dimension = editor.getComponent().getSize();
      return new Rectangle(point, dimension);
    }
    return null;
  }
}
