/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2020 The IdeaVim authors
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

package com.maddyhome.idea.vim.key;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * @author vlan
 */
public enum ShortcutOwner {
  UNDEFINED("undefined", "Undefined"),
  IDE(Constants.IDE_STRING, "IDE"),
  VIM(Constants.VIM_STRING, "Vim");

  private final @NotNull @NonNls String name;
  private final @NotNull @NonNls String title;

  ShortcutOwner(@NotNull @NonNls String name, @NotNull @NonNls String title) {
    this.name = name;
    this.title = title;
  }

  public static @NotNull ShortcutOwner fromString(@NotNull String s) {
    if (Constants.IDE_STRING.equals(s)) {
      return IDE;
    }
    else if (Constants.VIM_STRING.equals(s)) {
      return VIM;
    }
    return UNDEFINED;
  }

  @Override
  public @NotNull String toString() {
    return title;
  }

  public @NotNull String getName() {
    return name;
  }

  private static class Constants {
    @NonNls private static final String IDE_STRING = "ide";
    @NonNls private static final String VIM_STRING = "vim";
  }
}
