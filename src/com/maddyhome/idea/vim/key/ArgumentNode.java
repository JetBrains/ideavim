/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
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

import com.intellij.openapi.actionSystem.AnAction;
import com.maddyhome.idea.vim.command.Argument;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.CommandFlags;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Objects;

/**
 * This represents a command argument node in the key/action tree. Currently arguments of argType character
 * and visual command are used.
 */
public class ArgumentNode implements Node {
  /**
   * Creates a node for the given action.
   *
   * @param actionId The id of the action.
   * @param action   The action this arguments is mapped to.
   * @param cmdType  The type of the command this argument is for.
   * @param argType  The type of the argument.
   * @param flags    Any special flags associated with this argument.
   */
  public ArgumentNode(String actionId, AnAction action, @NotNull Command.Type cmdType, @NotNull Argument.Type argType,
                      EnumSet<CommandFlags> flags) {
    this.actionId = actionId;
    this.action = action;
    this.argType = argType;
    this.cmdType = cmdType;
    this.flags = flags;
  }

  public String getActionId() {
    return actionId;
  }

  /**
   * Gets the action of the argument
   *
   * @return The argument's action
   */
  public AnAction getAction() {
    return action;
  }

  /**
   * Gets the argument type
   *
   * @return The argument's type
   */
  @NotNull
  public Argument.Type getArgType() {
    return argType;
  }

  /**
   * Gets the type of the command this arguments is for
   *
   * @return The argument's command type
   */
  @NotNull
  public Command.Type getCmdType() {
    return cmdType;
  }

  /**
   * Gets the argument flags
   *
   * @return The argument's flags
   */
  public EnumSet<CommandFlags> getFlags() {
    return flags;
  }

  @NotNull
  public String toString() {

    return "ArgumentNode[" +
           "actionId=" +
           actionId +
           ", action=" +
           action +
           ", argType=" +
           argType +
           ", flags=" +
           flags +
           "]";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ArgumentNode that = (ArgumentNode) o;
    return Objects.equals(actionId, that.actionId) &&
            Objects.equals(action, that.action) &&
            argType == that.argType &&
            cmdType == that.cmdType &&
            Objects.equals(flags, that.flags);
  }

  @Override
  public int hashCode() {
    return Objects.hash(actionId, action, argType, cmdType, flags);
  }

  protected final String actionId;
  protected final AnAction action;
  @NotNull protected final Argument.Type argType;
  @NotNull protected final Command.Type cmdType;
  protected final EnumSet<CommandFlags> flags;
}
