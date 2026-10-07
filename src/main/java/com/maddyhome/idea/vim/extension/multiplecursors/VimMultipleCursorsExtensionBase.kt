/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.multiplecursors

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putExtensionHandlerMapping
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMapping
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMappingIfMissing

/**
 * Common base of the multiple cursors extensions
 *
 * vim-multiple-cursors and vim-visual-multi implement multiple cursors themselves. We use the native carets instead,
 * so the extensions share the handlers and differ mostly in their mappings.
 */
internal abstract class VimMultipleCursorsExtensionBase : VimExtension {

  protected fun putPlugMapping(modes: Set<MappingMode>, plug: String, handler: ExtensionHandler) {
    putExtensionHandlerMapping(modes, injector.parser.parseKeys(plug), owner, handler, false)
  }

  /**
   * Maps [keys] to [plug], unless the user has mapped other keys to [plug] already
   */
  protected fun putDefaultMapping(modes: Set<MappingMode>, keys: String, plug: String) {
    putKeyMappingIfMissing(modes, injector.parser.parseKeys(keys), owner, injector.parser.parseKeys(plug), true)
  }

  protected fun putMapping(modes: Set<MappingMode>, keys: String, plug: String) {
    putKeyMapping(modes, injector.parser.parseKeys(keys), owner, injector.parser.parseKeys(plug), true)
  }
}

/**
 * Whether a global flag variable, such as `g:multi_cursor_use_default_mapping`, is enabled. It is, unless it's `0`.
 */
internal fun isGlobalFlagEnabled(variableName: String): Boolean =
  VimPlugin.getVariableService().getGlobalVariableValue(variableName)?.toVimNumber()?.booleanValue ?: true
