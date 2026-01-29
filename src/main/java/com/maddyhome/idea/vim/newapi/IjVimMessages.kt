/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.maddyhome.idea.vim.api.MessageType
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimMessagesBase
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.EngineMessageHelper
import com.maddyhome.idea.vim.ui.ShowCmd
import java.awt.Toolkit

@Service
internal class IjVimMessages : VimMessagesBase() {

  private var message: String? = null
  private var error = false
  private var lastBeepTimeMillis = 0L

  override fun showMessage(editor: VimEditor, message: String?) {
    showMessageInternal(editor, message, MessageType.STANDARD)
  }

  override fun showErrorMessage(editor: VimEditor, message: String?) {
    showMessageInternal(editor, message, MessageType.ERROR)
    indicateError()
  }

  private fun showMessageInternal(editor: VimEditor, message: String?, messageType: MessageType) {
    this.message = message

    if (message.isNullOrBlank()) {
      clearStatusBarMessage()
      return
    }

    val context = injector.executionContextManager.getEditorExecutionContext(editor)
    injector.outputPanel.output(editor, context, message, messageType)
  }

  override fun getStatusBarMessage(): String? = message

  override fun clearStatusBarMessage() {
    if (message.isNullOrEmpty()) return
    injector.outputPanel.getCurrentOutputPanel()?.close()
    message = null
  }

  override fun indicateError() {
    error = true
    if (!ApplicationManager.getApplication().isUnitTestMode) {
      if (!injector.globalOptions().visualbell) {
        // Vim only allows a beep once every half second - :help 'visualbell'
        val currentTimeMillis = System.currentTimeMillis()
        if (currentTimeMillis - lastBeepTimeMillis > 500) {
          Toolkit.getDefaultToolkit().beep()
          lastBeepTimeMillis = currentTimeMillis
        }
      }
    }
  }

  override fun clearError() {
    error = false
  }

  override fun isError(): Boolean = error

  override fun message(key: String, vararg params: Any): String = EngineMessageHelper.message(key, *params)

  override fun updateStatusBar(editor: VimEditor) {
    ShowCmd.update()
  }
}
