package com.maddyhome.idea.vim.var;

import org.jetbrains.annotations.NotNull;

import java.util.EventObject;

/**
 * Created by psjay on 15/3/15.
 */
public class VariableChangeEvent extends EventObject {
  /**
   * Constructs a prototypical Event.
   *
   * @param source The object on which the Event initially occurred.
   * @throws IllegalArgumentException if source is null.
   */
  public VariableChangeEvent(Variable source) {
    super(source);
  }

  @NotNull
  public Variable getVariable() {
    return (Variable)this.getSource();
  }
}
