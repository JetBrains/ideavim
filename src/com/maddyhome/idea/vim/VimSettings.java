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

package com.maddyhome.idea.vim;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;

public class VimSettings {
  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public HashSet getChoices() {
    return choices;
  }

  public void setChoices(HashSet choices) {
    this.choices = choices;
  }

  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final VimSettings that = (VimSettings)o;

    if (enabled != that.enabled) {
      return false;
    }

    return choices.equals(that.choices);
  }

  public int hashCode() {
    int result;
    result = (enabled ? 1 : 0);
    result = 29 * result + choices.hashCode();
    return result;
  }

  @NotNull
  public String toString() {
    final StringBuffer sb = new StringBuffer();
    sb.append("VimSettings");
    sb.append("{enabled=").append(enabled);
    sb.append(", choices=").append(choices);
    sb.append('}');
    return sb.toString();
  }

  private boolean enabled;
  private HashSet choices = new HashSet();
}