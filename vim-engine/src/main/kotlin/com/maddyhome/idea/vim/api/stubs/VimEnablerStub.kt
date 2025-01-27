/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api.stubs

import com.maddyhome.idea.vim.api.VimEnabler
import com.maddyhome.idea.vim.diagnostic.vimLogger

class VimEnablerStub : VimEnabler {
  init {
    vimLogger<ExecutionContextManagerStub>().warn("VimEnablerStub is used. Please replace it with your own implementation of VimEnabler.")
  }

  override fun isEnabled(): Boolean = throw NotImplementedError()

  override fun isNewIdeaVimUser(): Boolean = throw NotImplementedError()
}
