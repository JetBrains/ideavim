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

package com.maddyhome.idea.vim.command;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This represents a command argument.
 */
public class Argument {

  /**
   * Creates a visual command argument
   *
   * @param motionArg The visual command
   */
  public Argument(@Nullable Command motionArg) {
    this.motionArg = motionArg;
    type = Type.MOTION;
  }

  /**
   * Creates a character argument
   *
   * @param charArg The character
   */
  public Argument(char charArg) {
    this.charArg = charArg;
    type = Type.CHARACTER;
  }

  /**
   * Returns the visual command if this is a visual command argument
   *
   * @return The visual command if this is a visual command argument, null if not
   */
  @Nullable
  public Command getMotion() {
    return motionArg;
  }

  /**
   * Creates a string argument
   *
   * @param strArg The string
   */
  public Argument(@Nullable String strArg) {
    this.strArg = strArg;
    type = Type.STRING;
  }

  /**
   * Returns the argument type
   *
   * @return The argument type
   */
  @NotNull
  public Type getType() {
    return type;
  }

  /**
   * Returns the character if this is a character argument
   *
   * @return The character if this is a character argument, null character if not
   */
  public char getCharacter() {
    return charArg;
  }

  public enum Type {
    NONE, MOTION, CHARACTER, STRING, DIGRAPH, EX_STRING
  }

  /**
   * Returns the string if this is a string argument
   *
   * @return The string if this is a string argument, null if not
   */
  @Nullable
  public String getString() {
    return strArg;
  }

  /**
   * Returns the argument as an object
   *
   * @return The argument (MOTION = Command, CHARACTER = Character, STRING = String)
   */
  @Nullable
  public Object getArgument() {
    switch (type) {
      case MOTION:
        return motionArg;
      case CHARACTER:
      case DIGRAPH:
        return charArg;
      case STRING:
        return strArg;
    }

    throw new IllegalStateException("Unexpected argType of " + type);
  }

  @NotNull
  public String toString() {

    return "Argument [" +
           "charArg=" +
           charArg +
           ", motionArg=" +
           motionArg +
           ", strArg=" +
           strArg +
           ", type=" +
           type +
           "]";
  }

  private char charArg = 0;
  @Nullable private Command motionArg = null;
  @Nullable private String strArg = null;
  @NotNull private final Type type;
}
