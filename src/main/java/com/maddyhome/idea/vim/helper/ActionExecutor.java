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
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Method;

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
      new AnActionEvent(null, context, ActionPlaces.KEYBOARD_SHORTCUT, action.getTemplatePresentation().clone(),
                        ActionManager.getInstance(), 0);

    if (!ActionUtil.lastUpdateAndCheckDumb(action, event, false)) {
      return false;
    }
    if (action instanceof ActionGroup && !canBePerformed(event, (ActionGroup) action, context)) {
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
      ActionUtil.performActionDumbAwareWithCallbacks(action, event);
      return true;
    }
  }

  private static boolean canBePerformed(AnActionEvent event, ActionGroup action, DataContext context) {
    Presentation presentation = event.getPresentation();
    try {
      Method isPerformGroup = Presentation.class.getMethod("isPerformGroup");
      return ((Boolean)isPerformGroup.invoke(presentation));
    }
    catch (Exception exception) {
      return action.canBePerformed(context);
    }
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
}
