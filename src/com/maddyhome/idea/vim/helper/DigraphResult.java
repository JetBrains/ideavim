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
