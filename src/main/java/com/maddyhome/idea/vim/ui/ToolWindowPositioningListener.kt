/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ui

import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import javax.swing.SwingUtilities

/**
 * Repositions a panel whenever a tool window visibility state changes.
 * Shared between [com.maddyhome.idea.vim.ui.ex.ExEntryPanel] and [OutputPanel].
 */
internal class ToolWindowPositioningListener(private val reposition: () -> Unit) : ToolWindowManagerListener {
  override fun stateChanged(toolWindowManager: ToolWindowManager) {
    SwingUtilities.invokeLater(reposition)
  }
}
