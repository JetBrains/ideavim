package com.maddyhome.idea.vim.var;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

/** Class represents Vim Variable which
 *  set by `let` command.
 *
 * Created by psjay on 15/3/14.
 */
public class Variable<T>{

  private String name;
  private T value;
  @NotNull
  private List<VariableChangeListener> listeners = new LinkedList<VariableChangeListener>();

  public Variable(String name) {
    this(name, null);
  }

  public Variable(String name, T value) {
    this.name = name;
    this.value = value;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public T getValue() {
    return value;
  }

  public void setValue(T value) {
    this.value = value;
    fireChangeEvent();
  }

  public void addVariableChangeListener(VariableChangeListener listener) {
    listeners.add(listener);
  }

  public boolean removeVariableChangeListener(VariableChangeListener listener) {
    return listeners.remove(listener);
  }

  private void fireChangeEvent() {
    VariableChangeEvent event = new VariableChangeEvent(this);
    for (VariableChangeListener l:listeners) {
      l.variableChange(event);
    }
  }

}
