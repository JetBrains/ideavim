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
  @NotNull ExtensionPointName<ExtensionBeanClass> EP_NAME = ExtensionPointName.create("IdeaVIM.vimExtension");

  @VimNlsSafe
  @NotNull String getName();

  default MappingOwner getOwner() {
    return MappingOwner.Plugin.Companion.get(getName());
  }

  /**
   * This method is always called AFTER the full execution of the `.ideavimrc` file.
   * <p>
   * During vim initialization process, it firstly loads the .vimrc file, then executes scripts from the plugins folder.
   * This practically means that the .vimrc file is initialized first; then the plugins are loaded.
   * See `:h initialization`
   * <p>
   * Why does this matter? Because this affects the order of commands are executed. For example,
   * ```
   * plug 'tommcdo/vim-exchange'
   * let g:exchange_no_mappings=1
   * ```
   * Here the user will expect that the exchange plugin won't have default mappings. However, if we load vim-exchange
   * immediately, this variable won't be initialized at the moment of plugin initialization.
   * <p>
   * There is also a tricky case for mappings override:
   * ```
   * plug 'tommcdo/vim-exchange'
   * map X <Plug>(ExchangeLine)
   * ```
   * For this case, a plugin with a good implementation detects that there is already a defined mapping for
   * `<Plug>(ExchangeLine)` and doesn't register the default cxx mapping. However, such detection requires the mapping
   * to be defined before the plugin initialization.
   */
  void init();

  default void dispose() {
    VimPlugin.getKey().removeKeyMapping(getOwner());
  }
}