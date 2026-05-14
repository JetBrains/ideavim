/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.classtextobj

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putExtensionHandlerMapping
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMappingIfMissing

/**
 * Provides a Vim-style text object for class definitions.
 *
 * Mapping:
 * - `ac` "a class definition" — the entire class definition (declaration line and body).
 */
internal class VimClassTextObjExtension : VimExtension {

  override fun getName(): String = "classtextobj"

  override fun init() {
    putExtensionHandlerMapping(
      MappingMode.XO,
      injector.parser.parseKeys(PLUG_OUTER_CLASS),
      owner,
      ClassTextObjectHandler(),
      false,
    )

    putKeyMappingIfMissing(
      MappingMode.XO,
      injector.parser.parseKeys("ac"),
      owner,
      injector.parser.parseKeys(PLUG_OUTER_CLASS),
      true,
    )
  }

  companion object {
    private const val PLUG_OUTER_CLASS = "<Plug>(textobj-class-ac)"
  }
}
