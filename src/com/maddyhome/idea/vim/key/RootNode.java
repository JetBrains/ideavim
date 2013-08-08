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

package com.maddyhome.idea.vim.key;

import org.jetbrains.annotations.NotNull;

/**
 * Represents the root of the key/action tree
 */
public class RootNode extends ParentNode {
  @NotNull
  public String toString() {
    StringBuffer res = new StringBuffer();
    res.append("RootNode[");
    res.append("children=[");
    int cnt = 0;
    for (Object key : children.keySet()) {
      Node node = children.get(key);
      if (cnt > 0) {
        res.append(", ");
      }
      res.append(key);
      res.append(" -> ");
      res.append(node);
      cnt++;
    }
    res.append("]");

    return res.toString();
  }
}
