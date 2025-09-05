/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension

import com.intellij.vim.api.VimApi
import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.thinapi.VimApiImpl

internal fun VimExtension.api(): VimApi = VimApiImpl(
  ListenerOwner.Plugin.get(this.name),
  MappingOwner.Plugin.get(this.name),
)