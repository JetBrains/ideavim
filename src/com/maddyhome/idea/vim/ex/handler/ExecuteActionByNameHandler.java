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

package com.maddyhome.idea.vim.ex.handler;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.ex.CommandHandler;
import com.maddyhome.idea.vim.ex.ExCommand;
import com.maddyhome.idea.vim.ex.ExException;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author smartbomb
 */
public class ExecuteActionByNameHandler extends CommandHandler {
  public ExecuteActionByNameHandler() {
    super("action", "", RANGE_FORBIDDEN | DONT_REOPEN);
  }

  public boolean execute(@NotNull Editor editor, @NotNull final DataContext context,
                         @NotNull ExCommand cmd) throws ExException {
    final String arg = cmd.getArgument().trim();
    final ActionManager aMgr = ActionManager.getInstance();
    final AnAction action = aMgr.getAction(arg);
    if (action == null) {
      VimPlugin.showMessage("Could not find action: " + arg);
      return false;
    }

    if (action.isInInjectedContext() || action.isEnabledInModalContext()) {
      final DataContext contentContext = DataManager.getInstance().getDataContext(editor.getContentComponent());
      try {
        KeyHandler.executeAction(action, contentContext);
      }
      catch (IllegalArgumentException ignored) {
      }
    }
    else {
      Timer t = new Timer(50, new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          try {
            KeyHandler.executeAction(action, context);
          }
          catch (IllegalArgumentException ignored) {
          }
        }
      });
      t.setRepeats(false);
      t.start();
    }

    return true;
  }
}
