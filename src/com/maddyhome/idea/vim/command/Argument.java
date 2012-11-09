package com.maddyhome.idea.vim.command;

/*
 * IdeaVim - A Vim emulator plugin for IntelliJ Idea
 * Copyright (C) 2003-2006 Rick Maddy
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

import org.jetbrains.annotations.NotNull;

/**
 * This represents a command argument.
 */
public class Argument {

  public static enum Type {
    NONE,
    MOTION,
    CHARACTER,
    STRING,
    DIGRAPH,
    EX_STRING;
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
   * Creates a motion command argument
   *
   * @param motionArg The motion command
   */
  public Argument(Command motionArg) {
    this.motionArg = motionArg;
    type = Type.MOTION;
  }

  /**
   * Creates a string argument
   *
   * @param strArg The string
   */
  public Argument(String strArg) {
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

  /**
   * Returns the motion command if this is a motion command argument
   *
   * @return The motion command if this is a motion command argument, null if not
   */
  public Command getMotion() {
    return motionArg;
  }

  /**
   * Returns the string if this is a string argument
   *
   * @return The string if this is a string argument, null if not
   */
  public String getString() {
    return strArg;
  }

  /**
   * Returns the argument as an object
   *
   * @return The argument (MOTION = Command, CHARACTER = Character, STRING = String)
   */
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

  public String toString() {
    StringBuffer res = new StringBuffer();
    res.append("Argument [");
    res.append("charArg=").append(charArg);
    res.append(", motionArg=").append(motionArg);
    res.append(", strArg=").append(strArg);
    res.append(", type=").append(type);
    res.append("]");

    return res.toString();
  }

  private char charArg = 0;
  private Command motionArg = null;
  private String strArg = null;
  @NotNull private Type type;
}
