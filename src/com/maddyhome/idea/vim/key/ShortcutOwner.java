package com.maddyhome.idea.vim.key;

import org.jetbrains.annotations.NotNull;

/**
 * @author vlan
 */
public enum ShortcutOwner {
  IDE("ide"),
  VIM("vim");

  @NotNull private final String name;

  ShortcutOwner(@NotNull String name) {
    this.name = name;
  }

  @NotNull
  public String getName() {
    return name;
  }
}
