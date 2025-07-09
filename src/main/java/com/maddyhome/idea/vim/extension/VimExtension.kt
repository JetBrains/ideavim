/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.extension

import com.intellij.openapi.extensions.ExtensionPointName
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.helper.VimNlsSafe
import com.maddyhome.idea.vim.key.MappingOwner

/**
 * @author vlan
 */
interface VimExtension {
  @get:VimNlsSafe
  val name: String

  val owner: MappingOwner
    get() = MappingOwner.Plugin.Companion.get(this.name)

  val listenerOwner: ListenerOwner
    get() = ListenerOwner.Plugin.Companion.get(this.name)

  /**
   * This method is always called AFTER the full execution of the `.ideavimrc` file.
   *
   *
   * During vim initialization process, it firstly loads the .vimrc file, then executes scripts from the plugins folder.
   * This practically means that the .vimrc file is initialized first; then the plugins are loaded.
   * See `:h initialization`
   *
   *
   * Why does this matter? Because this affects the order of commands are executed. For example,
   * ```
   * plug 'tommcdo/vim-exchange'
   * let g:exchange_no_mappings=1
   * ```
   * Here the user will expect that the exchange plugin won't have default mappings. However, if we load vim-exchange
   * immediately, this variable won't be initialized at the moment of plugin initialization.
   *
   *
   * There is also a tricky case for mappings override:
   * ```
   * plug 'tommcdo/vim-exchange'
   * map X <Plug>(ExchangeLine)
   * ```
   * For this case, a plugin with a good implementation detects that there is already a defined mapping for
   * `<Plug>(ExchangeLine)` and doesn't register the default cxx mapping. However, such detection requires the mapping
   * to be defined before the plugin initialization.
  </Plug></Plug> */
  fun init()

  fun dispose() {
    VimPlugin.getKey().removeKeyMapping(this.owner)
    injector.listenersNotifier.unloadListeners(this.listenerOwner)
  }

  companion object {
    internal val EP_NAME: ExtensionPointName<ExtensionBeanClass> = ExtensionPointName.create<ExtensionBeanClass>("IdeaVIM.vimExtension")
  }
}
