/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi

import com.intellij.vim.api.scopes.commandline.CommandLineTransaction
import com.maddyhome.idea.vim.api.VimCommandLine
import com.maddyhome.idea.vim.api.injector

class CommandLineTransactionImpl : CommandLineTransaction {
  private val activeCommandLine: VimCommandLine?
    get() = injector.commandLine.getActiveCommandLine()

  override suspend fun setText(text: String) {
    activeCommandLine?.setText(text)
  }

  override suspend fun insertText(offset: Int, text: String) {
    activeCommandLine?.insertText(offset, text)
  }

  override suspend fun setCaretPosition(position: Int) {
    activeCommandLine?.caret?.offset = position
  }

  override suspend fun close(refocusEditor: Boolean): Boolean {
    val commandLine = activeCommandLine ?: return false
    commandLine.close(refocusEditor, true)
    return true
  }
}