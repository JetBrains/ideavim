/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.helper

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.ui.KeyStrokeAdapter
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

/**
 * Extracts the [KeyStroke] of the key event that triggered an [AnActionEvent].
 */
internal class ActionEventKeyStrokeExtractor {
  private var keyStrokeCache: Pair<Long?, KeyStroke?> = null to null

  fun getKeyStroke(e: AnActionEvent): KeyStroke? {
    val inputEvent = e.inputEvent as? KeyEvent ?: return null
    val defaultKeyStroke = KeyStrokeAdapter.getDefaultKeyStroke(inputEvent)
    val strokeCache = keyStrokeCache
    if (defaultKeyStroke != null) {
      keyStrokeCache = inputEvent.`when` to defaultKeyStroke
      return defaultKeyStroke
    } else if (strokeCache.first == inputEvent.`when`) {
      keyStrokeCache = null to null
      return strokeCache.second
    }
    return KeyStroke.getKeyStrokeForEvent(inputEvent)
  }
}
