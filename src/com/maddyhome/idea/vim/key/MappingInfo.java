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

package com.maddyhome.idea.vim.key;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

/**
 * @author vlan
 */
public class MappingInfo {
  @NotNull private final List<KeyStroke> myFromKeys;
  @NotNull private final List<KeyStroke> myToKeys;
  private final boolean myRecursive;

  public MappingInfo(@NotNull List<KeyStroke> fromKeys, @NotNull List<KeyStroke> toKeys, boolean recursive) {
    myFromKeys = fromKeys;
    myToKeys = toKeys;
    myRecursive = recursive;
  }

  @NotNull
  public List<KeyStroke> getFromKeys() {
    return myFromKeys;
  }

  @NotNull
  public List<KeyStroke> getToKeys() {
    return myToKeys;
  }

  public boolean isRecursive() {
    return myRecursive;
  }
}
