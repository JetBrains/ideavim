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

package com.maddyhome.idea.vim.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.DumbAware;
import com.maddyhome.idea.vim.VimPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * This class is used to handle the Vim Plugin enabled/disabled toggle. This is most likely used as a menu option
 * but could also be used as a toolbar item.
 */
public class VimPluginToggleAction extends ToggleAction implements DumbAware {
  /**
   * Indicates if the toggle is on or off
   *
   * @param event The event that triggered the action
   * @return true if the toggle is on, false if off
   */
  @Override
  public boolean isSelected(@NotNull AnActionEvent event) {
    return VimPlugin.isEnabled();
  }

  /**
   * Specifies whether the toggle should be on or off
   *
   * @param event The event that triggered the action
   * @param b     The new state - true is on, false is off
   */
  @Override
  public void setSelected(@NotNull AnActionEvent event, boolean b) {
    VimPlugin.setEnabled(b);
  }
}
