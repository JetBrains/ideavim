/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.change.insert

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getLineEndForOffset
import com.maddyhome.idea.vim.common.TextRange

/**
 * Turns the text the IDE has just pasted between [start] and [end] in Replace mode into a replacement, as if it had been
 * typed.
 *
 * The IDE paste only inserts the text, so the characters that followed the caret now follow the pasted text. Like Vim,
 * each pasted character replaces one of them, until the end of the line, and the rest of the pasted text is appended. A
 * line break doesn't replace anything, it splits the line and replacing continues on the new line. Every pasted
 * character is recorded in the replace mask, so that backspace can put the original text back.
 */
fun replaceTextPastedInReplaceMode(editor: VimEditor, start: Int, end: Int) {
  val replaceMask = editor.replaceMask ?: return
  if (start >= end) return

  val text = editor.text()
  val pastedText = text.subSequence(start, end)
  val replacedCount = minOf(pastedText.count { it != '\n' }, editor.getLineEndForOffset(end) - end)
  val replacedText = text.subSequence(end, end + replacedCount).toString()

  var replacedIndex = 0
  pastedText.forEachIndexed { index, char ->
    if (char != '\n' && replacedIndex < replacedCount) {
      replaceMask.recordOverwriteAt(start + index, replacedText[replacedIndex++])
    } else {
      replaceMask.recordInsertAt(start + index)
    }
  }

  if (replacedCount > 0) {
    editor.deleteString(TextRange(end, end + replacedCount))
  }
}
