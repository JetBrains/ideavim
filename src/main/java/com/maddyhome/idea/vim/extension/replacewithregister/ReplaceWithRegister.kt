/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.replacewithregister

import com.intellij.vim.api.VimInitApi
import com.intellij.vim.api.scopes.nmapPluginAction
import com.intellij.vim.api.scopes.vmapPluginAction
import com.maddyhome.idea.vim.extension.VimExtension

internal class ReplaceWithRegister : VimExtension {

  override fun getName(): String = "ReplaceWithRegister"

  override fun init(initApi: VimInitApi) {
    initApi.mappings {
      nmapPluginAction("gr", RWR_OPERATOR, keepDefaultMapping = true) {
        rewriteMotion()
      }
      nmapPluginAction("grr", RWR_LINE, keepDefaultMapping = true) {
        rewriteLine()
      }
      vmapPluginAction("gr", RWR_VISUAL, keepDefaultMapping = true) {
        rewriteVisual()
      }
    }

    initApi.commands {
      exportOperatorFunction(OPERATOR_FUNC_NAME) {
        operatorFunction()
      }
    }
  }
}
