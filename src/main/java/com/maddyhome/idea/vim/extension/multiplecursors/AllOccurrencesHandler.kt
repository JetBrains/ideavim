/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.multiplecursors

import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.helper.inVisualMode
import com.maddyhome.idea.vim.helper.updateCaretsVisualAttributes
import com.maddyhome.idea.vim.newapi.vim

/**
 * Adds a cursor at every occurrence of the word under the caret, or of the selection in Visual mode
 */
internal class AllOccurrencesHandler(
  private val wholeWord: Boolean,
  private val caseSensitivity: CaseSensitivity,
) : WriteActionHandler() {

  override fun executeInWriteAction(editor: Editor): Boolean {
    if (editor.caretModel.caretCount > 1) return false

    val primaryCaret = editor.caretModel.primaryCaret
    val text = if (editor.inVisualMode) primaryCaret.selectedText else primaryCaret.wordUnderCaret()
    if (text == null) return false

    if (!editor.inVisualMode) {
      enterCharacterwiseVisualMode(editor.vim)
    }

    for (occurrence in findAllOccurrences(editor, text, wholeWord, caseSensitivity.ignoresCase(text))) {
      if (occurrence.contains(primaryCaret.offset)) {
        primaryCaret.vim.moveToOffset(occurrence.startOffset)
        primaryCaret.selectOccurrence(occurrence.startOffset, text)
      } else {
        val position = editor.offsetToVisualPosition(occurrence.startOffset)
        val caret = editor.caretModel.addCaret(position, true) ?: return false
        caret.selectOccurrence(occurrence.startOffset, text)
      }
    }
    editor.updateCaretsVisualAttributes()
    return true
  }
}
