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

import com.intellij.ide.DataManager;
import com.intellij.ide.util.gotoByName.GotoActionModel;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.wm.IdeFocusManager;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.ex.CommandHandler;
import com.maddyhome.idea.vim.ex.ExCommand;
import com.maddyhome.idea.vim.ex.ExException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;

/**
 * @author smartbomb
 */
public class ActionHandler extends CommandHandler {
  public ActionHandler() {
    super("action", "", RANGE_FORBIDDEN | DONT_REOPEN);
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
      executeAction(action, context, actionName);
    }
    else {
      final Component component = DataKeys.CONTEXT_COMPONENT.getData(context);
      ApplicationManager.getApplication().invokeLater(() -> IdeFocusManager.getInstance(DataKeys.PROJECT.getData(context)).doWhenFocusSettlesDown(
              () -> performAction(action, component, null)));
    }
    return true;
  }

  public static void performAction(Object element, @Nullable final Component component, @Nullable final AnActionEvent e) {
    performAction(element, component, e, null);
  }

  public static void performAction(Object element,
                                   @Nullable final Component component,
                                   @Nullable final AnActionEvent e,
                                   @Nullable final Runnable callback) {
    // element could be AnAction (SearchEverywhere)
    if (component == null) return;
    final AnAction action = element instanceof AnAction ? (AnAction)element : ((GotoActionModel.ActionWrapper)element).getAction();
    TransactionGuard.getInstance().submitTransactionLater(ApplicationManager.getApplication(), () -> {
      DataManager instance = DataManager.getInstance();
      DataContext context = instance != null ? instance.getDataContext(component) : DataContext.EMPTY_CONTEXT;
      InputEvent inputEvent = e == null ? null : e.getInputEvent();
      AnActionEvent event = AnActionEvent.createFromAnAction(action, inputEvent, ActionPlaces.ACTION_SEARCH, context);

      if (ActionUtil.lastUpdateAndCheckDumb(action, event, false)) {
        if (action instanceof ActionGroup && ((ActionGroup)action).getChildren(event).length > 0) {
          ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
            event.getPresentation().getText(), (ActionGroup)action, context, false, callback, -1);
          Window window = SwingUtilities.getWindowAncestor(component);
          if (window != null) {
            popup.showInCenterOf(window);
          }
          else {
            popup.showInFocusCenter();
          }
        }
        else {
          ActionUtil.performActionDumbAware(action, event);
          if (callback != null) callback.run();
        }
      }
    });
  }

  private void executeAction(@NotNull AnAction action, @NotNull DataContext context, @NotNull String actionName) {
    try {
      KeyHandler.executeAction(action, context);
    }
    catch (RuntimeException e) {
      // TODO: Find out if any runtime exceptions may happen here
      assert false : "Error while executing :action " + actionName + " (" + action + "): " + e;
    }
  }
}
