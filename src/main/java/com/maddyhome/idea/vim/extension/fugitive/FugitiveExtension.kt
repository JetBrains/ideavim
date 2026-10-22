/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.fugitive

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.VimExtensionFacade.addCommand

/**
 * Emulation of [vim-fugitive](https://github.com/tpope/vim-fugitive). Only `:Ggrep` is implemented.
 */
internal class FugitiveExtension : VimExtension {

  override fun getName(): String = "fugitive"

  override fun init() {
    addCommand(GREP_COMMAND, GrepCommand())
  }

  override fun dispose() {
    injector.commandGroup.removeAlias(GREP_COMMAND)
    super.dispose()
  }

  private companion object {
    const val GREP_COMMAND = "Ggrep"
  }
}
