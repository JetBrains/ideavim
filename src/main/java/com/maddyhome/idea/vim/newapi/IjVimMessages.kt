/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import com.maddyhome.idea.vim.api.MessageType
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimMessagesBase
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.EngineMessageHelper
import com.maddyhome.idea.vim.ui.ShowCmd

internal class IjVimMessages : VimMessagesBase() {

  private var message: String? = null
  private var error = false

  override fun showMessage(editor: VimEditor, message: String?) {
    injector.outputPanel.clear(editor, injector.executionContextManager.getEditorExecutionContext(editor))
    showMessageInternal(editor, message, MessageType.STANDARD)
  }

  override fun showErrorMessage(editor: VimEditor, message: String?) {
    injector.outputPanel.clear(editor, injector.executionContextManager.getEditorExecutionContext(editor))
    showMessageInternal(editor, message, MessageType.ERROR)
    // In Vim the bell of an error message only rings when 'errorbells' is set (:help 'errorbells')
    indicateError(ringBell = injector.globalOptions().errorbells)
  }

  override fun appendErrorMessage(editor: VimEditor, message: String?) {
    showMessageInternal(editor, message, MessageType.ERROR)
    indicateError(ringBell = injector.globalOptions().errorbells)
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

  @Suppress("DEPRECATION")
  override fun showStatusBarMessage(editor: VimEditor?, message: String?) {
    if (editor != null) {
      showMessage(editor, message)
    } else {
      // Legacy path for when editor is null - just store the message
      this.message = message
    }
  }

  override fun getStatusBarMessage(): String? = message

  override fun clearStatusBarMessage() {
    injector.outputPanel.getCurrentOutputPanel()?.close()
    message = null
  }

  override fun indicateError() {
    // An error without a message rings the bell whatever 'errorbells' is (:help 'errorbells')
    indicateError(ringBell = true)
  }

  private fun indicateError(ringBell: Boolean) {
    error = true
    // 'visualbell' silences both kinds of bell
    if (ringBell && !injector.globalOptions().visualbell) {
      injector.bell.ring()
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
