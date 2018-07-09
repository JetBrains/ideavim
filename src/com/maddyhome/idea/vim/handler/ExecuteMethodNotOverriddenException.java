package com.maddyhome.idea.vim.handler;

public class ExecuteMethodNotOverriddenException extends Exception {
  public ExecuteMethodNotOverriddenException(Class<?> child) {
    super("The proper execute() method is not overridden in " + child.getName());
  }
}
