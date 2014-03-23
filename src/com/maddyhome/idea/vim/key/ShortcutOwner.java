package com.maddyhome.idea.vim.key;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

  @Nullable
  public static ShortcutOwner fromString(@NotNull String s) {
    if ("ide".equals(s)) {
      return IDE;
    }
    else if ("vim".equals(s)) {
      return VIM;
    }
    return null;
  }
}
