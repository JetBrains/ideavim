/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.replacewithregister

import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.api

internal class ReplaceWithRegister : VimExtension {

  override fun getName(): String = "ReplaceWithRegister"

  override fun init() {
    val api = api()

    api.mappings {
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

      api.exportOperatorFunction(OPERATOR_FUNC_NAME) {
        operatorFunction()
      }
    }
  }
}
