/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.newapi

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.Service
import com.maddyhome.idea.vim.api.VimEditor
import java.awt.Component
import java.awt.Toolkit
import java.awt.Window
import java.awt.event.KeyEvent
import javax.swing.KeyStroke
import javax.swing.SwingUtilities

@Service
class IjVimApplication : VimApplication {
  override fun isMainThread(): Boolean {
    return ApplicationManager.getApplication().isDispatchThread
  }

  override fun invokeLater(action: () -> Unit, editor: VimEditor) {
    ApplicationManager.getApplication()
      .invokeLater(action, ModalityState.stateForComponent((editor as IjVimEditor).editor.component))
  }

  override fun isUnitTest(): Boolean {
    return ApplicationManager.getApplication().isUnitTestMode
  }

  override fun postKey(stroke: KeyStroke, editor: VimEditor) {
    val component: Component = SwingUtilities.getAncestorOfClass(Window::class.java, editor.ij.component)
    val event = createKeyEvent(stroke, component)
    ApplicationManager.getApplication().invokeLater {
      if (logger.isDebug()) {
        logger.debug("posting $event")
      }
      Toolkit.getDefaultToolkit().systemEventQueue.postEvent(event)
    }
  }

  private fun createKeyEvent(stroke: KeyStroke, component: Component): KeyEvent {
    return KeyEvent(component,
      if (stroke.keyChar == KeyEvent.CHAR_UNDEFINED) KeyEvent.KEY_PRESSED else KeyEvent.KEY_TYPED,
      System.currentTimeMillis(), stroke.modifiers, stroke.keyCode, stroke.keyChar)
  }

  companion object {
    private val logger = vimLogger<IjVimApplication>()
  }
}
