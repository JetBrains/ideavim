/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.multiplecursors

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.extension.VimExtensionFacade.executeNormalWithoutMapping
import com.maddyhome.idea.vim.newapi.ij
import javax.swing.KeyStroke

/**
 * Runs the [keys] without mappings, so they keep their Vim meaning, e.g. `n` still extends the Visual selection to the
 * next match
 */
internal class NativeKeysHandler(private val keys: List<KeyStroke>) : ExtensionHandler {

  override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
    executeNormalWithoutMapping(countKeys(operatorArguments.count0) + keys, editor.ij)
  }

  private fun countKeys(count: Int): List<KeyStroke> =
    if (count > 0) injector.parser.stringToKeys(count.toString()) else emptyList()
}
