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
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.options.helpers.KeywordOptionHelper

@CommandOrMotion(keys = ["<C-R><C-F>"], modes = [Mode.CMD_LINE])
class InsertFilenameUnderCaretAction : CommandLineActionHandler() {
  override fun execute(commandLine: VimCommandLine): Boolean {
    val editor = commandLine.editor
    val text = editor.text()
    val caretOffset = editor.primaryCaret().offset

    if (text.isEmpty()) {
      // E446: No file name under cursor
      injector.messages.showStatusBarMessage(commandLine.editor, injector.messages.message("E446"))
      return false
    }

    val start = if (!KeywordOptionHelper.isFilename(editor, text[caretOffset])) {
      moveForwardsToStartOfFilename(editor, text, caretOffset)
    } else {
      moveBackwardsToStartOfFilename(editor, text, caretOffset)
    }
    if (start == -1) {
      // E446: No file name under cursor
      injector.messages.showStatusBarMessage(commandLine.editor, injector.messages.message("E446"))
      return false
    }

    val end = moveForwardsToEndOfFilename(editor, text, start)
    if (end == -1) {
      // E446: No file name under cursor
      injector.messages.showStatusBarMessage(commandLine.editor, injector.messages.message("E446"))
      return false
    }

    val filename = text.substring(start, end + 1) // End exclusive
    commandLine.insertText(commandLine.caret.offset, filename)
    return true
  }

  private fun moveForwardsToStartOfFilename(editor: VimEditor, text: CharSequence, start: Int): Int {
    var offset = start
    while (offset < text.length && !KeywordOptionHelper.isFilename(editor, text[offset])) {
      if (text[offset] == '\n') return -1
      offset++
    }
    if (offset == text.length) return -1
    return offset
  }

  private fun moveBackwardsToStartOfFilename(editor: VimEditor, text: CharSequence, start: Int): Int {
    var offset = start
    while (offset > 0 && KeywordOptionHelper.isFilename(editor, text[offset - 1])) {
      if (text[offset - 1] == '\n') return -1
      offset--
    }
    return offset
  }

  private fun moveForwardsToEndOfFilename(editor: VimEditor, text: CharSequence, start: Int): Int {
    var offset = start
    while (offset < text.length && text[offset] != '\n' && KeywordOptionHelper.isFilename(editor, text[offset])) {
      offset++
    }
    return offset - 1
  }
}
