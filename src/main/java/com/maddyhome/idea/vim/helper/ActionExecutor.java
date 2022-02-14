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

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class ActionExecutor {
  /**
   * Execute an action
   *
   * @param action  The action to execute
   * @param context The context to run it in
   */
  @SuppressWarnings("deprecation")
  public static boolean executeAction(@NotNull AnAction action, @NotNull DataContext context) {
    final AnActionEvent event =
      new AnActionEvent(null, context, ActionPlaces.KEYBOARD_SHORTCUT, action.getTemplatePresentation(),
                        ActionManager.getInstance(), 0);

    if (action instanceof ActionGroup && !((ActionGroup)action).canBePerformed(context)) {
      // Some ActionGroups should not be performed, but shown as a popup
      ListPopup popup = JBPopupFactory.getInstance()
        .createActionGroupPopup(event.getPresentation().getText(), (ActionGroup)action, context, false, null, -1);

      Component component = context.getData(PlatformDataKeys.CONTEXT_COMPONENT);
      if (component != null) {
        Window window = SwingUtilities.getWindowAncestor(component);
        if (window != null) {
          popup.showInCenterOf(window);
        }
        return true;
      }
      popup.showInFocusCenter();
      return true;
    }
    else {
      // beforeActionPerformedUpdate should be called to update the action. It fixes some rider-specific problems.
      //   because rider use async update method. See VIM-1819.
      action.beforeActionPerformedUpdate(event);
      if (event.getPresentation().isEnabled()) {
        // Executing listeners for action. I can't be sure that this code is absolutely correct,
        //   action execution process in IJ seems to be more complicated.
        ActionManagerEx actionManager = ActionManagerEx.getInstanceEx();
        actionManager.fireBeforeActionPerformed(action, event);

        action.actionPerformed(event);

        actionManager.fireAfterActionPerformed(action, event, AnActionResult.PERFORMED);
        return true;
      }
    }
    return false;
  }

  /**
   * Execute an action by name
   *
   * @param name    The name of the action to execute
   * @param context The context to run it in
   */
  public static boolean executeAction(@NotNull @NonNls String name, @NotNull DataContext context) {
    ActionManager aMgr = ActionManager.getInstance();
    AnAction action = aMgr.getAction(name);
    return action != null && executeAction(action, context);
  }

  public static void executeCommand(@Nullable Project project,
                                    @NotNull Runnable runnable,
                                    @Nullable @NlsContexts.Command String name,
                                    @Nullable Object groupId) {
    CommandProcessor.getInstance().executeCommand(project, runnable, name, groupId);
  }

  public static boolean executeEsc(@NotNull DataContext context) {
    return ActionExecutor.executeAction(IdeActions.ACTION_EDITOR_ESCAPE, context);
  }
}
