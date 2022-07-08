package com.maddyhome.idea.vim.api.stubs

import com.maddyhome.idea.vim.api.VimEnabler
import com.maddyhome.idea.vim.diagnostic.vimLogger

class VimEnablerStub : VimEnabler {
  init {
    vimLogger<ExecutionContextManagerStub>().warn("VimEnablerStub is used. Please replace it with your own implementation of VimEnabler.")
  }

  override fun isEnabled(): Boolean {
    TODO("Not yet implemented")
  }
}
