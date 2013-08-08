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

package com.maddyhome.idea.vim.regexp;

public class Flags {
  public Flags() {
    flags = 0;
  }

  public Flags(int flags) {
    this.flags = flags;
  }

  public int get() {
    return flags;
  }

  public boolean isSet(int flag) {
    return ((this.flags & flag) != 0);
  }

  public boolean allSet(int flags) {
    return ((this.flags & flags) == flags);
  }

  public int init(int flags) {
    this.flags = flags;

    return this.flags;
  }

  public int set(int flags) {
    this.flags |= flags;

    return this.flags;
  }

  public int unset(int flags) {
    this.flags &= ~flags;

    return this.flags;
  }

  private int flags;
}
