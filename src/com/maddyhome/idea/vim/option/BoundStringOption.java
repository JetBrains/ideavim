/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2013 The IdeaVim authors
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

package com.maddyhome.idea.vim.option;

import org.jetbrains.annotations.NotNull;

public class BoundStringOption extends StringOption {
  BoundStringOption(String name, String abbrev, String dflt, String[] values) {
    super(name, abbrev, dflt);

    this.values = values;
  }

  public boolean set(String val) {
    if (isValid(val)) {
      return super.set(val);
    }

    return false;
  }

  public boolean append(String val) {
    if (isValid(val) && getValue().length() == 0) {
      return super.set(val);
    }

    return false;
  }

  public boolean prepend(String val) {
    if (isValid(val) && getValue().length() == 0) {
      return super.set(val);
    }

    return false;
  }

  public boolean remove(@NotNull String val) {
    if (getValue().equals(val)) {
      return super.remove(val);
    }

    return false;
  }

  private boolean isValid(String val) {
    for (String value : values) {
      if (value.equals(val)) {
        return true;
      }
    }

    return false;
  }

  protected String[] values;
}
