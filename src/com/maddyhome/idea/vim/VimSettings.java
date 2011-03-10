package com.maddyhome.idea.vim;

/*
 * IdeaVim - A Vim emulator plugin for IntelliJ Idea
 * Copyright (C) 2003-2005 Rick Maddy
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

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

  public boolean equals(Object o) {
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