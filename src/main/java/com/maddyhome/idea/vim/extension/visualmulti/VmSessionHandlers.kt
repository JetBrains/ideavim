/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.visualmulti

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.extension.VimExtensionFacade.executeNormalWithoutMapping
import com.maddyhome.idea.vim.helper.exitVisualMode
import com.maddyhome.idea.vim.newapi.ij
import javax.swing.KeyStroke

internal class StartSessionHandler(private val handler: ExtensionHandler) : ExtensionHandler {
  override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
    handler.execute(editor, context, operatorArguments)
    VmSession.startIfRegionsCreated(editor)
  }
}

/**
 * Runs [handler] inside the session. Outside it, runs the [keys] without mappings, e.g. `n` still extends the Visual
 * selection to the next match.
 */
internal class SessionOnlyHandler(private val handler: ExtensionHandler, private val keys: List<KeyStroke>) :
  ExtensionHandler {

  override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
    if (VmSession.isActive(editor)) {
      handler.execute(editor, context, operatorArguments)
    } else {
      executeNormalWithoutMapping(countKeys(operatorArguments.count0) + keys, editor.ij)
    }
  }

  private fun countKeys(count: Int): List<KeyStroke> =
    if (count > 0) injector.parser.stringToKeys(count.toString()) else emptyList()
}

/**
 * Keeps only the primary cursor and goes back to Normal mode
 */
internal class ExitHandler : ExtensionHandler {
  override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
    VmSession.end(editor)
    editor.removeSecondaryCarets()
    editor.exitVisualMode()
  }
}
