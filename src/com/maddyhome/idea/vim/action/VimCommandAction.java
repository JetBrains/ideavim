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

package com.maddyhome.idea.vim.action;

import com.google.common.collect.ImmutableSet;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.maddyhome.idea.vim.command.Argument;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.MappingMode;
import com.maddyhome.idea.vim.helper.StringHelper;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;
import java.util.Set;

/**
 * Action that represents a Vim command.
 *
 * Actions should be registered in resources/META-INF/plugin.xml and in package-info.java
 * inside {@link com.maddyhome.idea.vim.action}.
 *
 * @author vlan
 */
public abstract class VimCommandAction extends EditorAction {
  protected VimCommandAction(EditorActionHandler defaultHandler) {
    super(defaultHandler);
  }

  @NotNull
  public abstract Set<MappingMode> getMappingModes();

  @NotNull
  public abstract Set<List<KeyStroke>> getKeyStrokesSet();

  @NotNull
  public abstract Command.Type getType();

  @NotNull
  public Argument.Type getArgumentType() {
    return Argument.Type.NONE;
  }

  /**
   * Returns various binary flags for the command.
   *
   * These legacy flags will be refactored in future releases.
   *
   * @see com.maddyhome.idea.vim.command.Command
   */
  public int getFlags() {
    return 0;
  }

  @NotNull
  protected static Set<List<KeyStroke>> parseKeysSet(@NotNull String... keyStrings) {
    final ImmutableSet.Builder<List<KeyStroke>> builder = ImmutableSet.builder();
    for (String keyString : keyStrings) {
      builder.add(StringHelper.parseKeys(keyString));
    }
    return builder.build();
  }
}
