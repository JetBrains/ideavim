/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.autocmd

import com.maddyhome.idea.vim.api.AutoCmdService
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class AutoCmdImpl : AutoCmdService {

  private val eventHandlers: MutableMap<AutoCmdEvent, MutableList<AuCommand>> = ConcurrentHashMap()
  private var currentAugroup: String? = null

  override fun registerEventCommand(command: String, event: AutoCmdEvent, pattern: String) {
    eventHandlers.getOrPut(event.canonical) { CopyOnWriteArrayList() }
      .add(AuCommand(command, currentAugroup, AutoCmdPattern(pattern)))
  }

  override fun clearEvents() {
    val group = currentAugroup
    if (group != null) {
      clearAugroup(group)
      return
    }
    eventHandlers.clear()
  }

  override fun startAugroup(name: String) {
    currentAugroup = name
  }

  override fun endAugroup() {
    currentAugroup = null
  }

  override fun clearAugroup(name: String) {
    eventHandlers.values.forEach { handlers ->
      handlers.removeAll { it.group == name }
    }
  }

  override fun handleEvent(event: AutoCmdEvent, filePath: String?, editor: VimEditor?) {
    val resolvedEditor = editor ?: injector.editorGroup.getSelectedEditor() ?: return
    val path = filePath ?: resolvedEditor.getPath()
    eventHandlers[event.canonical]?.forEach { auCommand ->
      if (auCommand.pattern.matches(path)) {
        executeCommand(auCommand.command, resolvedEditor)
      }
    }
  }

  private fun executeCommand(command: String, editor: VimEditor) {
    val context = injector.executionContextManager.getEditorExecutionContext(editor)
    injector.vimscriptExecutor.execute(command, editor, context, skipHistory = true)
  }
}
