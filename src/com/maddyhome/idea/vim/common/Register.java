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

package com.maddyhome.idea.vim.common;

import com.intellij.codeInsight.editorActions.TextBlockTransferableData;
import com.maddyhome.idea.vim.command.SelectionType;
import com.maddyhome.idea.vim.helper.StringHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Represents a register.
 */
public class Register {
  private char name;
  @NotNull private final SelectionType type;
  @NotNull private final List<KeyStroke> keys;
  @NotNull private List<? extends TextBlockTransferableData> transferableData = new ArrayList<>();

  public Register(char name, @NotNull SelectionType type, @NotNull List<KeyStroke> keys) {
    this.name = name;
    this.type = type;
    this.keys = keys;
  }

  public Register(char name, @NotNull SelectionType type, @NotNull String text, @NotNull List<? extends TextBlockTransferableData> transferableData) {
    this.name = name;
    this.type = type;
    this.keys = StringHelper.stringToKeys(text);
    this.transferableData = transferableData;
  }

  public void rename(char name) {
    this.name = name;
  }

  /**
   * Get the name the register is assigned to.
   */
  public char getName() {
    return name;
  }

  @NotNull
  public List<? extends TextBlockTransferableData> getTransferableData() {
    return transferableData;
  }

  /**
   * Get the register type.
   */
  @NotNull
  public SelectionType getType() {
    return type;
  }

  /**
   * Get the text in the register.
   */
  @Nullable
  public String getText() {
    final StringBuilder builder = new StringBuilder();
    for (KeyStroke key : keys) {
      final char c = key.getKeyChar();
      if (c == KeyEvent.CHAR_UNDEFINED) {
        return null;
      }
      builder.append(c);
    }
    return builder.toString();
  }

  /**
   * Get the sequence of keys in the register.
   */
  @NotNull
  public List<KeyStroke> getKeys() {
    return keys;
  }

  /**
   * Append the supplied text to any existing text.
   */
  public void addTextAndResetTransferableData(@NotNull String text) {
    addKeys(StringHelper.stringToKeys(text));
    transferableData.clear();
  }

  public void addKeys(@NotNull List<KeyStroke> keys) {
    this.keys.addAll(keys);
  }

  public static class KeySorter implements Comparator<Register> {
    @Override
    public int compare(@NotNull Register o1, @NotNull Register o2) {
      return Character.compare(o1.name, o2.name);
    }
  }
}
