/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api.stubs

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimMessagesBase
import com.maddyhome.idea.vim.diagnostic.vimLogger

class VimMessagesStub : VimMessagesBase() {
  init {
    vimLogger<ExecutionContextManagerStub>().warn("VimMessagesStub is used. Please replace it with your own implementation of VimMessages.")
  }

  override fun showStatusBarMessage(editor: VimEditor?, message: String?) {
    TODO("Not yet implemented")
  }

  override fun getStatusBarMessage(): String? {
    TODO("Not yet implemented")
  }

  override fun clearStatusBarMessage() {
    TODO("Not yet implemented")
  }

  override fun indicateError() {
    TODO("Not yet implemented")
  }

  override fun clearError() {
    TODO("Not yet implemented")
  }

  override fun isError(): Boolean {
    TODO("Not yet implemented")
  }

  override fun message(key: String, vararg params: Any): String {
    TODO("Not yet implemented")
  }

  override fun updateStatusBar(editor: VimEditor) {
    TODO("Not yet implemented")
  }
}
