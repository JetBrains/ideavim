/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.windownavigation

import com.intellij.openapi.components.service
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ToolWindowType
import com.maddyhome.idea.vim.extension.VimExtension
import java.awt.Component
import java.awt.KeyboardFocusManager
import java.beans.PropertyChangeListener
import javax.swing.JComponent
import javax.swing.SwingUtilities

internal class ToolWindowNavEverywhere : VimExtension {
  companion object {
    const val PLUGIN_NAME = "ToolWindowNavEverywhere"
  }

  override fun getName() = PLUGIN_NAME

  val focusListener = PropertyChangeListener { evt ->
    val newFocusOwner = evt.newValue as? JComponent
    val oldFocusOwner = evt.oldValue as? JComponent
    val dispatcher = service<ToolWindowNavDispatcher>()

    if (newFocusOwner != null && isInsideToolWindow(newFocusOwner)) {
      dispatcher.register(newFocusOwner)
    }

    if (oldFocusOwner != null && isInsideToolWindow(oldFocusOwner)) {
      dispatcher.unregisterCustomShortcutSet(oldFocusOwner)
    }
  }

  override fun init() {
    KeyboardFocusManager.getCurrentKeyboardFocusManager()
      .addPropertyChangeListener("focusOwner", focusListener)
  }

  override fun dispose() {
    KeyboardFocusManager.getCurrentKeyboardFocusManager()
      .removePropertyChangeListener("focusOwner", focusListener)
    super.dispose()
  }

  private fun isInsideToolWindow(component: Component): Boolean {
    for (project in ProjectManager.getInstance().openProjects) {
      if (project.isDisposed) continue
      val toolWindowManager = ToolWindowManager.getInstance(project)
      for (id in toolWindowManager.toolWindowIds) {
        val toolWindow = toolWindowManager.getToolWindow(id) ?: continue
        if (!toolWindow.isVisible) continue
        if (toolWindow.type == ToolWindowType.FLOATING || toolWindow.type == ToolWindowType.WINDOWED) continue
        if (SwingUtilities.isDescendingFrom(component, toolWindow.component)) {
          return true
        }
      }
    }
    return false
  }
}
