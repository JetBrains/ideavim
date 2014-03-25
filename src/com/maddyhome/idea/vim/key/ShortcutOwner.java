package com.maddyhome.idea.vim.key;

import org.jetbrains.annotations.NotNull;

/**
 * @author vlan
 */
public enum ShortcutOwner {
  UNDEFINED("undefined", "Undefined"),
  IDE("ide", "IDE"),
  VIM("vim", "Vim");

  @NotNull private final String name;
  @NotNull private final String title;

  ShortcutOwner(@NotNull String name, @NotNull String title) {
    this.name = name;
    this.title = title;
  }

  @Override
  public String toString() {
    return title;
  }

  @NotNull
  public String getName() {
    return name;
  }

  @NotNull
  public static ShortcutOwner fromString(@NotNull String s) {
    if ("ide".equals(s)) {
      return IDE;
    }
    else if ("vim".equals(s)) {
      return VIM;
    }
    return UNDEFINED;
  }
}
