/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.ex

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.VimCommandLine

@CommandOrMotion(keys = ["<C-U>"], modes = [Mode.CMD_LINE])
class DeleteToCaretAction : CommandLineActionHandler() {
  override fun execute(commandLine: VimCommandLine): Boolean {
    val caretOffset = commandLine.caret.offset
    if (caretOffset == 0) return true

    commandLine.deleteText(0, caretOffset)
    return true
  }
}
