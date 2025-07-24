/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.thin.api

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.CommandAliasHandler
import com.maddyhome.idea.vim.extension.VimExtensionFacade
import com.maddyhome.idea.vim.extension.exportOperatorFunction
import com.maddyhome.idea.vim.key.OperatorFunction
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.thinapi.VimPluginService

class VimPluginServiceImpl : VimPluginService {
  override fun executeNormalWithoutMapping(command: String, editor: VimEditor) {
    VimExtensionFacade.executeNormalWithoutMapping(injector.parser.parseKeys(command), editor.ij)
  }

  override fun exportOperatorFunction(name: String, function: OperatorFunction) {
    VimExtensionFacade.exportOperatorFunction(name, function)
  }

  override fun addCommand(name: String, commandHandler: CommandAliasHandler) {
    VimExtensionFacade.addCommand(name, commandHandler)
  }
}