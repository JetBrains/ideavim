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

package com.maddyhome.idea.vim.group;

import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.command.SelectionType;
import com.maddyhome.idea.vim.common.Register;
import com.maddyhome.idea.vim.common.TextRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

public interface VimRegisterGroup {
  /**
   * Check to see if the last selected register can be written to.
   */
  boolean isRegisterWritable();

  boolean isValid(char reg);

  boolean selectRegister(char reg);

  void resetRegister();

  void resetRegisters();

  boolean storeText(@NotNull Editor editor, @NotNull TextRange range, @NotNull SelectionType type, boolean isDelete);

  boolean storeTextSpecial(char register, @NotNull String text);

  @Nullable Register getLastRegister();

  @Nullable Register getPlaybackRegister(char r);

  @Nullable Register getRegister(char r);

  void saveRegister(char r, Register register);

  char getCurrentRegister();

  char getDefaultRegister();

  @NotNull List<Register> getRegisters();

  boolean startRecording(Editor editor, char register);

  void recordKeyStroke(@NotNull KeyStroke key);

  void recordText(@NotNull String text);

  void setKeys(char register, @NotNull List<KeyStroke> keys);

  void setKeys(char register, @NotNull List<KeyStroke> keys, SelectionType type);

  void finishRecording(Editor editor);
}
