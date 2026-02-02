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

  private val eventHandlers: MutableMap<AutoCmdEvent, MutableList<AuCommand>> = mutableMapOf()
  private var currentAugroup: String? = null

  override fun registerEventCommand(command: String, event: AutoCmdEvent) {
    eventHandlers.getOrPut(event) { mutableListOf() }.add(AuCommand(command, currentAugroup))
  }

  override fun clearEvents() {
    if (currentAugroup != null) {
      clearAuGroup(currentAugroup!!)
      return
    }
    eventHandlers.clear()
  }

  override fun startAugroup(name: String) {
    currentAugroup = name
  }

  override fun endAuGroup() {
    currentAugroup = null
  }

  override fun clearAuGroup(name: String) {
    eventHandlers.values.forEach { handlers ->
      val iterator = handlers.iterator()
      while (iterator.hasNext()) {
        val handler = iterator.next()
        if (handler.group == name) {
          iterator.remove()
        }
      }
    }
  }

  override fun handleEvent(event: AutoCmdEvent) {
    eventHandlers[event]?.forEach { executeCommand(it.command) }
  }

  private fun executeCommand(command: String) {
    val editor = injector.editorGroup.getFocusedEditor() ?: return
    val context = injector.executionContextManager.getEditorExecutionContext(editor)
    injector.vimscriptExecutor.execute(command, editor, context, skipHistory = true)
  }
}