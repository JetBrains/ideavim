/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.textobjuser

import com.intellij.vim.api.VimInitApi
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.extension.VimExtension

/**
 * A port of kana/vim-textobj-user: a framework that lets users declaratively define their own text objects via
 * `textobj#user#plugin({name}, {specs})`.
 *
 * See: https://github.com/kana/vim-textobj-user
 */
internal class VimTextObjUserExtension : VimExtension {

  override fun getName(): String = "textobj-user"

  override fun init(initApi: VimInitApi) {
    injector.functionService.registerFunctionHandler(
      FUNCTION_NAME,
      TextObjUserPluginFunctionHandler(FUNCTION_NAME, getOwner()),
    )
  }

  override fun dispose() {
    injector.functionService.unregisterFunctionHandler(FUNCTION_NAME)
  }

  companion object {
    private const val FUNCTION_NAME = "textobj#user#plugin"
  }
}
