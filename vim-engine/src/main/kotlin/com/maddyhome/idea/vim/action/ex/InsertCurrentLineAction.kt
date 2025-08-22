/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.ex

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.VimCommandLine

@CommandOrMotion(keys = ["<C-R><C-L>"], modes = [Mode.CMD_LINE])
class InsertCurrentLineAction : CommandLineActionHandler() {
  override fun execute(commandLine: VimCommandLine): Boolean {
    val text = commandLine.editor.getLineText(commandLine.editor.primaryCaret().getBufferPosition().line)
    commandLine.insertText(commandLine.caret.offset, text)
    return true
  }
}
