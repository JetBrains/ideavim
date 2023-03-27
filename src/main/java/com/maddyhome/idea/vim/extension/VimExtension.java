/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.helper.VimNlsSafe;
import com.maddyhome.idea.vim.key.MappingOwner;
import org.jetbrains.annotations.NotNull;

/**
 * @author vlan
 */
public interface VimExtension {
  @NotNull
  ExtensionPointName<ExtensionBeanClass> EP_NAME = ExtensionPointName.create("IdeaVIM.vimExtension");

  @VimNlsSafe
  @NotNull
  String getName();

  default MappingOwner getOwner() {
    return MappingOwner.Plugin.Companion.get(getName());
  }

  void init();

  default void dispose() {
    VimPlugin.getKey().removeKeyMapping(getOwner());
  }
}
