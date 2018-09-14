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

package com.maddyhome.idea.vim.key;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.command.SelectionType;
import org.jetbrains.annotations.NotNull;

/**
 * @author vlan
 */
public interface OperatorFunction {
  /**
   * The value of 'operatorfunc' to be used as the operator function in 'g@'.
   *
   * Make sure to synchronize your function properly using read/write actions.
   */
  boolean apply(@NotNull Editor editor, @NotNull DataContext context, @NotNull SelectionType selectionType);
}
