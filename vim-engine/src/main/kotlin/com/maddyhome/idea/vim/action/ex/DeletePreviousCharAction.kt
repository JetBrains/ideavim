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
import com.maddyhome.idea.vim.common.Graphemes

@CommandOrMotion(keys = ["<BS>", "<C-H>"], modes = [Mode.CMD_LINE])
class DeletePreviousCharAction : CommandLineActionHandler() {
  override fun execute(commandLine: VimCommandLine): Boolean {
    val oldText = commandLine.text
    if (oldText.isEmpty()) {
      commandLine.close(refocusOwningEditor = true, resetCaret = false)
      return true
    }

    val caretOffset = commandLine.caret.offset
    if (caretOffset == 0) return true

    val prevOffset = Graphemes.prev(oldText, caretOffset) ?: 0
    commandLine.deleteText(prevOffset, caretOffset - prevOffset)

    return true
  }
}
