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
      nmap(keys = "gr", actionName = RWR_OPERATOR) {
        rewriteMotion()
      }
      nmap(keys = "grr", actionName = RWR_LINE) {
        rewriteLine()
      }
      vmap(keys = "gr", actionName = RWR_VISUAL) {
        rewriteVisual()
      }

      api.exportOperatorFunction(OPERATOR_FUNC_NAME) {
        operatorFunction()
      }
    }
  }
}
