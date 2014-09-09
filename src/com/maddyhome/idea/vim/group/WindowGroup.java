/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2014 The IdeaVim authors
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

package com.maddyhome.idea.vim.group;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.maddyhome.idea.vim.VimPlugin;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 *
 */
public class WindowGroup {
  public WindowGroup() {
  }

  public void splitWindowHorizontal(@NotNull DataContext context, String filename) {
    splitWindow(SwingConstants.HORIZONTAL, context, filename);
  }

  public void splitWindowVertical(@NotNull DataContext context, String filename) {
    splitWindow(SwingConstants.VERTICAL, context, filename);
  }

  private void splitWindow(int orientation, @NotNull DataContext context, String filename) {
    Project proj = PlatformDataKeys.PROJECT.getData(context);
    FileEditorManagerEx fem = FileEditorManagerEx.getInstanceEx(proj);

    // If a file was passed in as an argument, open it in the newly split window
    VirtualFile virtualFile = null;
    if (filename != null && filename.length() > 0)
    {
      virtualFile = VimPlugin.getFile().findFile(filename, proj);

      // Don't split if the desired file could not be found
      if (virtualFile == null) {
        VimPlugin.showMessage("Could not find file: " + filename);
        return;
      }
    }

    EditorWindow editorWindow = fem.getSplitters().getCurrentWindow();

    if (editorWindow != null ) {
      // When virtualFile is null, the current file will be split
      editorWindow.split(orientation, true, virtualFile, true);
    }
  }
}
