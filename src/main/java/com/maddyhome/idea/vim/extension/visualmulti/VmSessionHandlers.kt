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
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.extension.multiplecursors.leaveVisualMode

internal class StartSessionHandler(private val handler: ExtensionHandler) : ExtensionHandler {
  override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
    handler.execute(editor, context, operatorArguments)
    VmSession.startIfRegionsCreated(editor)
  }
}

/**
 * Runs [handler] inside the session, and [outsideSession] outside it
 */
internal class SessionOnlyHandler(
  private val handler: ExtensionHandler,
  private val outsideSession: ExtensionHandler,
) : ExtensionHandler {

  override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
    val active = if (VmSession.isActive(editor)) handler else outsideSession
    active.execute(editor, context, operatorArguments)
  }
}

/**
 * Keeps only the primary cursor and goes back to Normal mode. The regions can be selected again with Reselect Last.
 */
internal class ExitHandler : ExtensionHandler {
  override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
    saveLastRegions(editor)
    VmSession.end(editor)
    editor.removeSecondaryCarets()
    leaveVisualMode(editor)
  }
}
