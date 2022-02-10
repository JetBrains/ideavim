/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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

package com.maddyhome.idea.vim.helper;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.DocCommandGroupId;
import com.intellij.openapi.project.Project;
import com.maddyhome.idea.vim.command.OperatorArguments;
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import static com.intellij.openapi.actionSystem.CommonDataKeys.*;
import static com.intellij.openapi.actionSystem.PlatformCoreDataKeys.PROJECT_FILE_DIRECTORY;

public class VimActionExecutor {
  public static void executeVimAction(@NotNull Editor editor,
                                      @NotNull EditorActionHandlerBase cmd,
                                      DataContext context,
                                      @NotNull OperatorArguments operatorArguments) {
    CommandProcessor.getInstance()
      .executeCommand(editor.getProject(), () -> cmd.execute(editor, getProjectAwareDataContext(editor, context), operatorArguments),
                      cmd.getId(), DocCommandGroupId.noneGroupId(editor.getDocument()), UndoConfirmationPolicy.DEFAULT,
                      editor.getDocument());
  }

  // This method is copied from com.intellij.openapi.editor.actionSystem.EditorAction.getProjectAwareDataContext
  private static @NotNull DataContext getProjectAwareDataContext(final @NotNull Editor editor,
                                                                 final @NotNull DataContext original) {
    if (PROJECT.getData(original) == editor.getProject()) {
      return new DialogAwareDataContext(original);
    }

    return dataId -> {
      if (PROJECT.is(dataId)) {
        final Project project = editor.getProject();
        if (project != null) {
          return project;
        }
      }
      return original.getData(dataId);
    };

  }

  // This class is copied from com.intellij.openapi.editor.actionSystem.DialogAwareDataContext.DialogAwareDataContext
  private static final class DialogAwareDataContext implements DataContext {
    @SuppressWarnings("rawtypes")
    private static final DataKey[] keys = {PROJECT, PROJECT_FILE_DIRECTORY, EDITOR, VIRTUAL_FILE, PSI_FILE};
    private final Map<String, Object> values = new HashMap<>();

    DialogAwareDataContext(DataContext context) {
      for (DataKey<?> key : keys) {
        values.put(key.getName(), key.getData(context));
      }
    }

    @Override
    public @Nullable Object getData(@NotNull @NonNls String dataId) {
      if (values.containsKey(dataId)) {
        return values.get(dataId);
      }
      final Editor editor = (Editor)values.get(EDITOR.getName());
      if (editor != null) {
        return DataManager.getInstance().getDataContext(editor.getContentComponent()).getData(dataId);
      }
      return null;
    }
  }
}
