package com.maddyhome.idea.vim.option;

import com.maddyhome.idea.vim.editor.linenumber.LineNumbers;

/**
 * Adds a listener to enable / disable the relative
 * line numbers when the 'relativenumber' option changes.
 */
public class RelativeLineNumberOption extends ToggleOption {

  public static final String NAME = "relativenumber";
  public static final String ABBREV = "rnu";

  public RelativeLineNumberOption() {
    super(NAME, ABBREV, false);
    addOptionChangeListener(new OptionChangeListener() {
      @Override
      public void valueChange(OptionChangeEvent event) {
        LineNumbers.getInstance().refresh();
      }
    });
  }
}
