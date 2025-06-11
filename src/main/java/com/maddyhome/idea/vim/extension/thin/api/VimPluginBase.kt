/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.thin.api

import com.intellij.vim.api.scopes.VimScope
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.thinapi.VimScopeImpl

abstract class VimPluginBase : VimExtension {
  override fun init() {
    val vimScope = VimScopeImpl(listenerOwner, mappingOwner = owner)
    vimScope.init()
  }

  final override fun dispose() {
    val vimScope = VimScopeImpl(listenerOwner, mappingOwner = owner)
    vimScope.unload()

    super.dispose()
  }

  abstract fun VimScope.init()

  open fun VimScope.unload() {}
}