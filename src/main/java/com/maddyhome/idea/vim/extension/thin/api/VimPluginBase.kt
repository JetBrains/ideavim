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
  final override suspend fun init() {
    val vimScope = VimScopeImpl(listenerOwner, mappingOwner = mappingOwner)
    vimScope.init()
  }

  final override fun dispose() {
    val vimScope = VimScopeImpl(listenerOwner, mappingOwner = mappingOwner)
    vimScope.unload()

    super.dispose()
  }

  abstract suspend fun VimScope.init()

  open fun VimScope.unload() {}
}