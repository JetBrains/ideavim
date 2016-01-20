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

import org.jetbrains.annotations.NotNull;

/**
 * @author vlan
 */
public enum ShortcutOwner {
  UNDEFINED("undefined", "Undefined"),
  IDE("ide", "IDE"),
  VIM("vim", "Vim");

  @NotNull private final String name;
  @NotNull private final String title;

  ShortcutOwner(@NotNull String name, @NotNull String title) {
    this.name = name;
    this.title = title;
  }

  @NotNull
  @Override
  public String toString() {
    return title;
  }

  @NotNull
  public String getName() {
    return name;
  }

  @NotNull
  public static ShortcutOwner fromString(@NotNull String s) {
    if ("ide".equals(s)) {
      return IDE;
    }
    else if ("vim".equals(s)) {
      return VIM;
    }
    return UNDEFINED;
  }
}
