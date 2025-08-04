/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.scripting.ide

import com.intellij.vim.api.scopes.VimScope
import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.thinapi.VimScopeImpl

fun getVimScope(): VimScope {
  val mappingOwner = MappingOwner.Plugin.get("my_name")
  val listenerOwner = ListenerOwner.Plugin.get("my_name")
  return VimScopeImpl(listenerOwner, mappingOwner)
}