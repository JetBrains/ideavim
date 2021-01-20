/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.helper;

import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class DigraphResult {
  public static final int RES_HANDLED = 0;
  public static final int RES_UNHANDLED = 1;
  public static final int RES_DONE = 3;
  public static final int RES_BAD = 4;

  static final DigraphResult HANDLED_DIGRAPH = new DigraphResult(RES_HANDLED, '?');
  static final DigraphResult HANDLED_LITERAL = new DigraphResult(RES_HANDLED, '^');
  static final DigraphResult UNHANDLED = new DigraphResult(RES_UNHANDLED);
  static final DigraphResult BAD = new DigraphResult(RES_BAD);

  private DigraphResult(int result) {
    this.result = result;
    stroke = null;
  }

  private DigraphResult(int result, char promptCharacter) {
    this.result = result;
    this.promptCharacter = promptCharacter;
    stroke = null;
  }

  private DigraphResult(@Nullable KeyStroke stroke) {
    result = RES_DONE;
    this.stroke = stroke;
  }

  public static DigraphResult done(@Nullable KeyStroke stroke) {
    return new DigraphResult(stroke);
  }

  public static DigraphResult handled(char promptCharacter) {
    return new DigraphResult(RES_HANDLED, promptCharacter);
  }

  public @Nullable KeyStroke getStroke() {
    return stroke;
  }

  public int getResult() {
    return result;
  }

  public char getPromptCharacter() {
    return promptCharacter;
  }

  private final int result;
  private final @Nullable KeyStroke stroke;
  private char promptCharacter;
}
