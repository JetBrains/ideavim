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

/**
 * Insert the filename at or following the caret to the command line.
 *
 * This implementation is used for both inserting normally, and inserting literally. A filename can't (or at least
 * shouldn't) contain control characters, so it can only be inserted literally, as plain text.
 */
@CommandOrMotion(keys = ["<C-R><C-F>", "<C-R><C-R><C-F>", "<C-R><C-O><C-F>"], modes = [Mode.CMD_LINE])
class InsertFilenameUnderCaretAction : CommandLineActionHandler() {
  override fun execute(commandLine: VimCommandLine): Boolean {
    val editor = commandLine.editor

    val offset = injector.searchGroup.getCurrentIncsearchResultRange(editor)?.endOffset ?: editor.primaryCaret().offset
    val range = injector.searchHelper.findFilenameAtOrFollowingCursor(editor, offset)
    if (range == null) {
      injector.messages.showErrorMessage(commandLine.editor, injector.messages.message("E446"))
      return false
    }

    val text = editor.text()
    val filename = text.substring(range.startOffset, range.endOffset + 1) // End exclusive
    commandLine.insertText(commandLine.caret.offset, filename)
    return true
  }
}
