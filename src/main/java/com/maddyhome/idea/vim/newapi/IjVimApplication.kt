/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.Service
import com.intellij.openapi.util.Computable
import com.intellij.util.ExceptionUtil
import com.maddyhome.idea.vim.api.VimApplicationBase
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.diagnostic.vimLogger
import java.awt.Component
import java.awt.Toolkit
import java.awt.Window
import java.awt.event.KeyEvent
import javax.swing.KeyStroke
import javax.swing.SwingUtilities

@Service
internal class IjVimApplication : VimApplicationBase() {
  override fun isMainThread(): Boolean {
    return ApplicationManager.getApplication().isDispatchThread
  }

  override fun invokeLater(action: () -> Unit, editor: VimEditor) {
    ApplicationManager.getApplication()
      .invokeLater(action, ModalityState.stateForComponent((editor as IjVimEditor).editor.component))
  }

  override fun invokeLater(action: () -> Unit) {
    ApplicationManager.getApplication().invokeLater(action)
  }

  override fun isUnitTest(): Boolean {
    return ApplicationManager.getApplication().isUnitTestMode
  }

  override fun isInternal(): Boolean {
    return ApplicationManager.getApplication().isInternal
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

  override fun <T> runWriteAction(action: () -> T): T {
    return ApplicationManager.getApplication().runWriteAction(Computable(action))
  }

  override fun <T> runReadAction(action: () -> T): T {
    return ApplicationManager.getApplication().runReadAction(Computable(action))
  }

  override fun currentStackTrace(): String {
    return ExceptionUtil.currentStackTrace()
  }

  override fun runAfterGotFocus(runnable: Runnable) {
    com.maddyhome.idea.vim.helper.runAfterGotFocus(runnable)
  }

  override fun isOctopusEnabled(): Boolean {
    val property = System.getProperty("octopus.handler") ?: "true"
    if (property.isBlank()) return true
    return property.toBoolean()
  }

  private fun createKeyEvent(stroke: KeyStroke, component: Component): KeyEvent {
    return KeyEvent(
      component,
      if (stroke.keyChar == KeyEvent.CHAR_UNDEFINED) KeyEvent.KEY_PRESSED else KeyEvent.KEY_TYPED,
      System.currentTimeMillis(),
      stroke.modifiers,
      stroke.keyCode,
      stroke.keyChar,
    )
  }

  companion object {
    private val logger = vimLogger<IjVimApplication>()
  }
}
