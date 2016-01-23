package com.maddyhome.idea.vim.action.plugin.surround;

/**
 * @author dhleong
 */
class SurroundPair {
  final String before, after;

  SurroundPair(String before, String after) {
    this.before = before;
    this.after = after;
  }

  public SurroundPair(char before, char after) {
    this(String.valueOf(before), String.valueOf(after));
  }

  @Override
  public String toString() {
    return "before=" + before + "  after=" + after;
  }
}
