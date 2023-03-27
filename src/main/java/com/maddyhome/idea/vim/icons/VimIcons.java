/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.icons;

import com.intellij.ui.IconManager;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public final class VimIcons {
  public static final @NotNull
  Icon IDEAVIM = load("/icons/ideavim.svg");
  public static final @NotNull
  Icon IDEAVIM_DISABLED = load("/icons/ideavim_disabled.svg");
  public static final @NotNull
  Icon TWITTER = load("/icons/twitter.svg");
  public static final @NotNull
  Icon YOUTRACK = load("/icons/youtrack.svg");

  private static @NotNull
  Icon load(@NotNull @NonNls String path) {
    return IconManager.getInstance().getIcon(path, VimIcons.class);
  }
}
