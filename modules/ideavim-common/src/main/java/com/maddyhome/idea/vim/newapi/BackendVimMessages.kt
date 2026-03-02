/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimMessagesBase
import com.maddyhome.idea.vim.helper.EngineMessageHelper

/**
 * Minimal [com.maddyhome.idea.vim.api.VimMessages] implementation used when the full
 * frontend (UI) messages service is not available (e.g. on the backend in split mode).
 *
 * The frontend module overrides this with [IjVimMessages] which adds status-bar
 * updates, beep sounds, and ShowCmd integration.
 */
class BackendVimMessages : VimMessagesBase() {

  private var message: String? = null
  private var error = false

  override fun showMessage(editor: VimEditor, message: String?) {
    this.message = message
  }

  override fun showErrorMessage(editor: VimEditor, message: String?) {
    this.message = message
    error = true
  }

  @Suppress("DEPRECATION")
  override fun showStatusBarMessage(editor: VimEditor?, message: String?) {
    this.message = message
  }

  override fun getStatusBarMessage(): String? = message

  override fun clearStatusBarMessage() {
    message = null
  }

  override fun indicateError() {
    error = true
  }

  override fun clearError() {
    error = false
  }

  override fun isError(): Boolean = error

  override fun message(key: String, vararg params: Any): String = EngineMessageHelper.message(key, *params)

  override fun updateStatusBar(editor: VimEditor) {
    // No-op: ShowCmd/status bar widgets are only available on the frontend
  }
}
