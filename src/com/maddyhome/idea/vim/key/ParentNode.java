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
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

/**
 * This abstract node is used as a base for any node that can contain child nodes
 */
public abstract class ParentNode implements Node {
  /**
   * This adds a child node keyed by the supplied key
   *
   * @param child The child node
   * @param key   The key to map the child to
   */
  public void addChild(@NotNull Node child, @NotNull Object key) {
    children.put(key, child);
  }

  /**
   * Returns the child node associated with the supplied key. The key must be the same as used in {@link #addChild}
   *
   * @param key The key used to find the child
   * @return The child mapped to key or null if no such mapping found
   */
  @Nullable
  public Node getChild(@NotNull Object key) {
    return children.get(key);
  }

  @NotNull protected HashMap<Object, Node> children = new HashMap<Object, Node>();
}
