/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.nerdtree

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.ui.treeStructure.Tree
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.extension.ShortcutDispatcher
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

/**
 * Handles keyboard shortcuts and delegates them to appropriate actions.
 */
internal abstract class AbstractDispatcher(name: String, mappings: Map<List<KeyStroke>, NerdTreeAction>) :
  ShortcutDispatcher<NerdTreeAction>(name, mappings, NerdTreeListener) {

  private object NerdTreeListener : Listener<NerdTreeAction> {
    override fun onMatch(e: AnActionEvent, keyStrokes: MutableList<KeyStroke>, data: NerdTreeAction) {
      val component = e.getData(PlatformDataKeys.CONTEXT_COMPONENT)
      if (component is Tree) {
        data.action(e, component)
      } else {
        LOG.error("Component is not a tree: $component")
      }

      keyStrokes.clear()
    }

    override fun onInvalid(e: AnActionEvent, keyStrokes: MutableList<KeyStroke>) {
      keyStrokes.clear()
      injector.messages.indicateError()
    }
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = true
    // If <ESC> is pressed, clear the keys; skip only if there are no keys
    if ((e.inputEvent as? KeyEvent)?.keyCode == KeyEvent.VK_ESCAPE) {
      e.presentation.isEnabled = keyStrokes.isNotEmpty()
      keyStrokes.clear()
    }
    // Skip if SpeedSearch is active
    if (e.getData(PlatformDataKeys.SPEED_SEARCH_TEXT) != null) {
      e.presentation.isEnabled = false
    }
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  companion object {
    val LOG = vimLogger<AbstractDispatcher>()
  }
}
