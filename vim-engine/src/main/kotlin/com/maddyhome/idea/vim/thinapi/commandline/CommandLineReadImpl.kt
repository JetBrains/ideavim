/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi.commandline

import com.intellij.vim.api.scopes.commandline.CommandLineRead
import com.maddyhome.idea.vim.api.VimCommandLine
import com.maddyhome.idea.vim.api.injector

class CommandLineReadImpl : CommandLineRead {
  private val activeCommandLine: VimCommandLine?
    get() = injector.commandLine.getActiveCommandLine()

  override val text: String
    get() = activeCommandLine?.text ?: ""

  override val caretPosition: Int
    get() = activeCommandLine?.caret?.offset ?: 0

  override val isActive: Boolean
    get() = activeCommandLine != null
}