/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.multiplecursors

import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.vim

/**
 * Removes the last added cursor. Removing the only one leaves Visual mode.
 */
internal class RemoveOccurrenceHandler : WriteActionHandler() {

  override fun executeInWriteAction(editor: Editor): Boolean {
    val caret = editor.caretModel.primaryCaret
    if (caret.selectedText == null) return false

    if (editor.caretModel.removeCaret(caret)) {
      // The search continues from the cursor that is the last one now
      val lastOccurrence = editor.caretModel.primaryCaret.selectedRange
      editor.occurrenceSearch = editor.occurrenceSearch?.copy(lastOccurrence = lastOccurrence)
    } else {
      leaveVisualMode(editor.vim)
    }
    injector.scroll.scrollCaretIntoView(editor.vim)
    return true
  }
}
