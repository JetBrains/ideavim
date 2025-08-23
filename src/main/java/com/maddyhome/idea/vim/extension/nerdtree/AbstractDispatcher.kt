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
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.ui.KeyStrokeAdapter
import com.intellij.ui.treeStructure.Tree
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.diagnostic.vimLogger
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

/**
 * Handles keyboard shortcuts and delegates them to appropriate actions.
 */
internal abstract class AbstractDispatcher(private val mappings: Mappings) : DumbAwareAction() {
  private val keys = mutableListOf<KeyStroke>()

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = e.getData(PlatformDataKeys.SPEED_SEARCH_TEXT) == null // SpeedSearch is inactive
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  override fun actionPerformed(e: AnActionEvent) {
    var keyStroke = getKeyStroke(e) ?: return
    val keyChar = keyStroke.keyChar
    if (keyChar != KeyEvent.CHAR_UNDEFINED) {
      keyStroke = KeyStroke.getKeyStroke(keyChar)
    }
    keys.add(keyStroke)

    val action = mappings.getAction(keys)
    if (action != null) {
      val component = e.getData(PlatformDataKeys.CONTEXT_COMPONENT)
      if (component is Tree) {
        action.action(e, component)
      } else {
        LOG.error("Component is not a tree: $component")
      }

      keys.clear()
    } else if (!mappings.isPrefix(keys)) { // invalid
      LOG.info("Unrecognized key sequence: $keys")
      keys.clear()
      injector.messages.indicateError()
    }
  }

  /**
   * getDefaultKeyStroke is needed for NEO layout keyboard VIM-987
   * but we should cache the value because on the second call (isEnabled -> actionPerformed)
   * the event is already consumed
   */
  private var keyStrokeCache: Pair<KeyEvent?, KeyStroke?> = null to null

  private fun getKeyStroke(e: AnActionEvent): KeyStroke? {
    val inputEvent = e.inputEvent
    if (inputEvent is KeyEvent) {
      val defaultKeyStroke = KeyStrokeAdapter.getDefaultKeyStroke(inputEvent)
      val strokeCache = keyStrokeCache
      if (defaultKeyStroke != null) {
        keyStrokeCache = inputEvent to defaultKeyStroke
        return defaultKeyStroke
      } else if (strokeCache.first === inputEvent) {
        keyStrokeCache = null to null
        return strokeCache.second
      }
      return KeyStroke.getKeyStrokeForEvent(inputEvent)
    }
    return null
  }

  companion object {
    val LOG = vimLogger<AbstractDispatcher>()
  }
}
