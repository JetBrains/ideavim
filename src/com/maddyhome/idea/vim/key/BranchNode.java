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

import javax.swing.*;

/**
 * This node of the key/action tree will contain one or more child nodes.
 */
public class BranchNode extends ParentNode {
  /**
   * This is a special key for an argument child node
   */
  public static final String ARGUMENT = "argument";

  /**
   * Creates the branch node for the given keystroke
   *
   * @param key The keystroke to get to this node
   */
  public BranchNode(KeyStroke key) {
    this(key, 0);
  }

  public BranchNode(KeyStroke key, int flags) {
    this.key = key;
    this.flags = flags;
  }

  /**
   * Returns the child node associated with the supplied key. The key must be the same as used in {@link #addChild}.
   * If no such child is found but there is an argument node, the argument node is returned.
   *
   * @param key The key used to find the child
   * @return The child mapped to key or an argument node or null if no such mapping found
   */
  @Nullable
  public Node getChild(@NotNull Object key) {
    Node res = super.getChild(key);
    if (res == null) {
      res = children.get(ARGUMENT);
    }

    return res;
  }

  public Node getArgumentNode() {
    return children.get(ARGUMENT);
  }

  /**
   * The key this node is associated with
   *
   * @return The node's keystroke
   */
  public KeyStroke getKey() {
    return key;
  }

  public int getFlags() {
    return flags;
  }

  @NotNull
  public String toString() {
    StringBuffer res = new StringBuffer();
    res.append("BranchNode[key=");
    res.append(key);
    res.append(", children=[");
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

  protected KeyStroke key;
  protected int flags;
}
