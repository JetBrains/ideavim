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
import com.maddyhome.idea.vim.common.Graphemes

@CommandOrMotion(keys = ["<DEL>"], modes = [Mode.CMD_LINE])
class DeleteNextCharAction : CommandLineActionHandler() {
  override fun execute(commandLine: VimCommandLine): Boolean {
    val caretOffset = commandLine.caret.offset

    val oldText = commandLine.text
    if (oldText.isEmpty()) {
      commandLine.close(refocusOwningEditor = true, resetCaret = false)
      return true
    }

    // If the caret is at the end of the text, delete the previous character
    if (caretOffset == oldText.length) {
      val preEndOffset = Graphemes.prev(oldText, oldText.length) ?: return true
      commandLine.deleteText(preEndOffset, oldText.length - preEndOffset)
    }
    else {
      val nextOffset = Graphemes.next(oldText, caretOffset) ?: return true
      commandLine.deleteText(caretOffset, nextOffset - caretOffset)
    }

    return true
  }
}
