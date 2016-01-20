/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2016 The IdeaVim authors
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

package com.maddyhome.idea.vim.ex;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.helper.EditorData;
import com.maddyhome.idea.vim.ui.ExOutputPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author vlan
 */
public class ExOutputModel {
  @NotNull private final Editor myEditor;
  @Nullable private String myText;

  private ExOutputModel(@NotNull Editor editor) {
    myEditor = editor;
  }

  @NotNull
  public static ExOutputModel getInstance(@NotNull Editor editor) {
    ExOutputModel model = EditorData.getExOutputModel(editor);
    if (model == null) {
      model = new ExOutputModel(editor);
      EditorData.setExOutputModel(editor, model);
    }
    return model;
  }

  public void output(@NotNull String text) {
    myText = text;
    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      ExOutputPanel.getInstance(myEditor).setText(text);
    }
  }

  public void clear() {
    myText = null;
    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      ExOutputPanel.getInstance(myEditor).deactivate(false);
    }
  }

  @Nullable
  public String getText() {
    return myText;
  }
}
