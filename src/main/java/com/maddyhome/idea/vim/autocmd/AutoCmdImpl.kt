/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.autocmd

import com.maddyhome.idea.vim.api.AutoCmdService
import com.maddyhome.idea.vim.api.injector

class AutoCmdImpl : AutoCmdService {

  private val eventHandlers: MutableMap<AutoCmdEvent, MutableList<String>> = mutableMapOf()

  override fun registerEventCommand(command: String, event: AutoCmdEvent) {
    eventHandlers.getOrPut(event) { mutableListOf() }.add(command)
  }

  override fun clearEvents() {
    eventHandlers.clear()
  }

  override fun handleEvent(event: AutoCmdEvent) {
    eventHandlers[event]?.forEach { executeCommand(it) }
  }

  private fun executeCommand(command: String) {
    val editor = injector.editorGroup.getFocusedEditor() ?: return
    val context = injector.executionContextManager.getEditorExecutionContext(editor)
    injector.vimscriptExecutor.execute(command, editor, context, skipHistory = true)
  }
}