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

package com.maddyhome.idea.vim.ex;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

/**
 *
 */
public class CommandNode {
  public CommandNode() {
    command = null;
  }

  public CommandNode(CommandHandler command) {
    this.command = command;
  }

  @NotNull
  public CommandNode addChild(char ch, CommandHandler command) {
    CommandNode res = new CommandNode(command);
    nodes.put(ch, res);

    return res;
  }

  public CommandNode getChild(char ch) {
    return nodes.get(ch);
  }

  @Nullable
  public CommandHandler getCommandHandler() {
    return command;
  }

  public void setCommandHandler(CommandHandler command) {
    this.command = command;
  }

  @Nullable private CommandHandler command;
  @NotNull private HashMap<Character, CommandNode> nodes = new HashMap<Character, CommandNode>();
}
