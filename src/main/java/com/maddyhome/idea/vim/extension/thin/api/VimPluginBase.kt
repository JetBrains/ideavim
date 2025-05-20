/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.thin.api

import com.intellij.vim.api.VimPluginApi
import com.intellij.vim.api.scopes.VimInitPluginScope
import com.intellij.vim.api.scopes.vimInitPluginScope
import com.maddyhome.idea.vim.extension.VimExtension

val vimPluginApi: VimPluginApi = VimPluginApiImpl()

interface VimPluginBase : VimExtension {
  override fun init() {
    vimInitPluginScope(vimPluginApi) {
      init()
    }
  }

  fun VimInitPluginScope.init()
}