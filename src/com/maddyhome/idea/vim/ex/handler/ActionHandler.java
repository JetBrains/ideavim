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

package com.maddyhome.idea.vim.ex.handler;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.ex.CommandHandler;
import com.maddyhome.idea.vim.ex.ExCommand;
import com.maddyhome.idea.vim.ex.ExException;
import com.maddyhome.idea.vim.helper.UiHelper;
import org.jetbrains.annotations.NotNull;

/**
 * @author smartbomb
 */
public class ActionHandler extends CommandHandler {
  public ActionHandler() {
    super("action", "", RANGE_OPTIONAL | DONT_REOPEN);
  }

  public boolean execute(@NotNull Editor editor, @NotNull final DataContext context,
                         @NotNull ExCommand cmd) throws ExException {
    final String actionName = cmd.getArgument().trim();
    final AnAction action = ActionManager.getInstance().getAction(actionName);
    if (action == null) {
      VimPlugin.showMessage("Action not found: " + actionName);
      return false;
    }
    final Application application = ApplicationManager.getApplication();
    if (application.isUnitTestMode()) {
      executeAction(editor, cmd, action, context, actionName);
    }
    else {
      UiHelper.runAfterGotFocus(() -> executeAction(editor, cmd, action, context, actionName));
    }
    return true;
  }

  private void executeAction(@NotNull Editor editor, @NotNull ExCommand cmd, @NotNull AnAction action,
                             @NotNull DataContext context, @NotNull String actionName) {
    final boolean visualAction = cmd.getRanges().size() > 0;
    if (visualAction) {
      VimPlugin.getMotion().selectPreviousVisualMode(editor);
    }
    try {
      KeyHandler.executeAction(action, context);
    }
    catch (RuntimeException e) {
      assert false : "Error while executing :action " + actionName + " (" + action + "): " + e;
    }
    finally {
      if (visualAction) {
        // Exit visual mode selected above, but do it without resetting the selected text
        CommandState.getInstance(editor).popState();
      }
    }
  }
}
