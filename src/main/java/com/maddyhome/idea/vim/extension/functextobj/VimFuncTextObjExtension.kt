/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.functextobj

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putExtensionHandlerMapping
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMappingIfMissing

/**
 * Provides Vim-style text objects for function/method definitions.
 *
 * Inspired by kana/vim-textobj-function: https://github.com/kana/vim-textobj-function
 *
 * Mappings:
 * - `am` "a method definition" — the function including signature and body, excluding JavaDoc
 *   and any annotations directly preceding it.
 * - `aM` "a Method definition" — same as `am` but including JavaDoc and annotations.
 * - `im` "inner method definition" — only the contents between the body braces.
 */
internal class VimFuncTextObjExtension : VimExtension {

  override fun getName(): String = "functextobj"

  override fun init() {
    putExtensionHandlerMapping(
      MappingMode.XO,
      injector.parser.parseKeys(PLUG_OUTER_METHOD),
      owner,
      FuncTextObjectHandler(FuncRange.OUTER_NO_DOC),
      false,
    )
    putExtensionHandlerMapping(
      MappingMode.XO,
      injector.parser.parseKeys(PLUG_OUTER_METHOD_WITH_DOC),
      owner,
      FuncTextObjectHandler(FuncRange.OUTER_WITH_DOC),
      false,
    )
    putExtensionHandlerMapping(
      MappingMode.XO,
      injector.parser.parseKeys(PLUG_INNER_METHOD),
      owner,
      FuncTextObjectHandler(FuncRange.INNER),
      false,
    )

    putKeyMappingIfMissing(
      MappingMode.XO,
      injector.parser.parseKeys("am"),
      owner,
      injector.parser.parseKeys(PLUG_OUTER_METHOD),
      true,
    )
    putKeyMappingIfMissing(
      MappingMode.XO,
      injector.parser.parseKeys("aM"),
      owner,
      injector.parser.parseKeys(PLUG_OUTER_METHOD_WITH_DOC),
      true,
    )
    putKeyMappingIfMissing(
      MappingMode.XO,
      injector.parser.parseKeys("im"),
      owner,
      injector.parser.parseKeys(PLUG_INNER_METHOD),
      true,
    )
  }

  companion object {
    private const val PLUG_OUTER_METHOD = "<Plug>(textobj-function-am)"
    private const val PLUG_OUTER_METHOD_WITH_DOC = "<Plug>(textobj-function-aM)"
    private const val PLUG_INNER_METHOD = "<Plug>(textobj-function-im)"
  }
}
