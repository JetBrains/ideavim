/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.replacewithregister

import com.intellij.vim.api.VimInitApi
import com.maddyhome.idea.vim.extension.VimExtension

internal class ReplaceWithRegister : VimExtension {

  override fun getName(): String = "ReplaceWithRegister"

  override fun init(initApi: VimInitApi) {
    initApi.mappings {
      // Step 1: Non-recursive <Plug> → action mappings
      nnoremap(RWR_OPERATOR) {
        rewriteMotion()
      }
      nnoremap(RWR_LINE) {
        rewriteLine()
      }
      vnoremap(RWR_VISUAL) {
        rewriteVisual()
      }

      // Step 2: Recursive key → <Plug> mappings
      nmap("gr", RWR_OPERATOR)
      nmap("grr", RWR_LINE)
      vmap("gr", RWR_VISUAL)
    }

    initApi.commands {
      exportOperatorFunction(OPERATOR_FUNC_NAME) {
        operatorFunction()
      }
    }
  }
}
