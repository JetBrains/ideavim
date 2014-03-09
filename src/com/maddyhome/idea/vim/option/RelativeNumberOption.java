package com.maddyhome.idea.vim.option;

import com.maddyhome.idea.vim.editor.relativenumber.RelativeLineNumbers;

/**
 * Adds a listener to enable / disable the relative
 * line numbers when the 'relativenumber' option changes.
 */
public class RelativeNumberOption extends ToggleOption {

  public RelativeNumberOption() {
    super("relativenumber", "rn", true);
    addOptionChangeListener(new OptionChangeListener() {
      @Override
      public void valueChange(OptionChangeEvent event) {
        RelativeLineNumbers.getInstance().refresh();
      }
    });
  }
}
