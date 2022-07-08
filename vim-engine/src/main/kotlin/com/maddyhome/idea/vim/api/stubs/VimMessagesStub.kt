package com.maddyhome.idea.vim.api.stubs

import com.maddyhome.idea.vim.api.VimMessagesBase
import com.maddyhome.idea.vim.diagnostic.vimLogger

class VimMessagesStub : VimMessagesBase() {
  init {
    vimLogger<ExecutionContextManagerStub>().warn("VimMessagesStub is used. Please replace it with your own implementation of VimMessages.")
  }

  override fun showStatusBarMessage(message: String?) {
    TODO("Not yet implemented")
  }

  override fun getStatusBarMessage(): String? {
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

  override fun updateStatusBar() {
    TODO("Not yet implemented")
  }
}
