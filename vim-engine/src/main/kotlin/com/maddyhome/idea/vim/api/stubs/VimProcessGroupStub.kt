/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api.stubs

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimProcessGroupBase
import com.maddyhome.idea.vim.diagnostic.vimLogger

class VimProcessGroupStub : VimProcessGroupBase() {
  init {
    vimLogger<ExecutionContextManagerStub>().warn("VimProcessGroupStub is used. Please replace it with your own implementation of VimProcessGroup.")
  }

  override fun executeCommand(
    editor: VimEditor,
    command: String,
    input: CharSequence?,
    currentDirectoryPath: String?,
  ): String? {
    TODO("Not yet implemented")
  }
}
