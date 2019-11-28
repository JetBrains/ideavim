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

package com.maddyhome.idea.vim.extension;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import org.jetbrains.annotations.NotNull;

/**
 * An action handler registered by its extension to be executed as a Vim command.
 *
 * @author vlan
 */
public interface VimExtensionHandler {
  /**
   * Executes the action.
   *
   * The action is run on the EDT thread inside the {@link com.intellij.openapi.command.CommandProcessor}.
   *
   * It's run without any read or write actions of {@link com.intellij.openapi.application.Application}, so you have to
   * make sure your code is synchronized properly. A read action is not needed for the EDT in the IntelliJ platform. As
   * for a write action, you'll have to apply it by yourself if you're modifying IntelliJ's data structures like
   * documents or virtual files.
   */
  void execute(@NotNull Editor editor, @NotNull DataContext context);

  default boolean isRepeatable() {
    return false;
  }
}
