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
import com.maddyhome.idea.vim.state.mode.Mode
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
    val handlers = eventHandlers[event.canonical] ?: return
    val context = injector.executionContextManager.getEditorExecutionContext(resolvedEditor)
    handlers.forEach { auCommand ->
      if (auCommand.pattern.matches(path)) {
        // exit insert mode if event must be run from normal mode
        if (event.runsInNormalMode && resolvedEditor.mode.isInsertOrReplace) {
          injector.changeGroup.processEscape(resolvedEditor, context)
        }
        injector.vimscriptExecutor.execute(auCommand.command, resolvedEditor, context, skipHistory = true)
      }
    }
  }
}

/**
 * Whether this event fires in Vim only while in Normal mode (so we must restore Normal mode if the IDE left us in
 * Insert mode). Insert-mode events and focus events legitimately occur during Insert mode and must not force an exit.
 */
private val AutoCmdEvent.runsInNormalMode: Boolean
  get() = when (this) {
    AutoCmdEvent.InsertEnter, AutoCmdEvent.InsertLeave, AutoCmdEvent.FocusGained, AutoCmdEvent.FocusLost -> false
    else -> true
  }

// Vim treats Replace mode like Insert for these purposes (`:help InsertEnter`).
private val Mode.isInsertOrReplace: Boolean
  get() = this is Mode.INSERT || this is Mode.REPLACE
