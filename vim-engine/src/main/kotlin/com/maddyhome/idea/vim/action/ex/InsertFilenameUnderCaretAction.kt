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
import com.maddyhome.idea.vim.api.injector

@CommandOrMotion(keys = ["<C-R><C-F>"], modes = [Mode.CMD_LINE])
class InsertFilenameUnderCaretAction : CommandLineActionHandler() {
  override fun execute(commandLine: VimCommandLine): Boolean {
    val editor = commandLine.editor

    val range = injector.searchHelper.findFilenameAtOrFollowingCursor(editor, editor.primaryCaret())
    if (range == null) {
      // E446: No file name under cursor
      injector.messages.showStatusBarMessage(commandLine.editor, injector.messages.message("E446"))
      return false
    }

    val text = editor.text()
    val filename = text.substring(range.startOffset, range.endOffset + 1) // End exclusive
    commandLine.insertText(commandLine.caret.offset, filename)
    return true
  }
}
