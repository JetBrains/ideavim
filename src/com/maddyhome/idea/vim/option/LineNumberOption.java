package com.maddyhome.idea.vim.option;

import com.maddyhome.idea.vim.editor.linenumber.LineNumbers;

/**
 * Adds a listener to enable / disable the  line
 * numbers when the 'number' option changes.
 */
public class LineNumberOption extends ToggleOption {

  public static final String NAME = "number";
  public static final String ABBREV = "nu";

  public LineNumberOption() {
    super(NAME, ABBREV, false);
    addOptionChangeListener(new OptionChangeListener() {
      @Override
      public void valueChange(OptionChangeEvent event) {
        LineNumbers.getInstance().refresh();
      }
    });
  }
}
