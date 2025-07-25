/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.common.CommandAliasHandler
import com.maddyhome.idea.vim.key.OperatorFunction

interface VimPluginService {
  fun executeNormalWithoutMapping(command: String, editor: VimEditor)
  fun exportOperatorFunction(name: String, function: OperatorFunction)
  fun addCommand(name: String, commandHandler: CommandAliasHandler)
}