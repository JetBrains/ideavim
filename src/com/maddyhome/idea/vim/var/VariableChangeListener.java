package com.maddyhome.idea.vim.var;

import java.util.EventListener;

/**
 * Created by psjay on 15/3/15.
 */
public interface VariableChangeListener extends EventListener {
  public void variableChange(VariableChangeEvent event);
}
