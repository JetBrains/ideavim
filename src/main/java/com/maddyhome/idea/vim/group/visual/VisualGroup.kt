/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.visual

import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.api.getLineEndForOffset
import com.maddyhome.idea.vim.api.getLineStartForOffset
import com.maddyhome.idea.vim.helper.isEndAllowed
import com.maddyhome.idea.vim.helper.moveToInlayAwareOffset
import com.maddyhome.idea.vim.helper.vimSelectionStart
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.inBlockSelection

internal fun moveCaretOneCharLeftFromSelectionEnd(editor: Editor, predictedMode: Mode) {
  if (predictedMode !is Mode.VISUAL) {
    if (!editor.vim.isEndAllowed(predictedMode)) {
      editor.caretModel.allCarets.forEach { caret ->
        val lineEnd = IjVimEditor(editor).getLineEndForOffset(caret.offset)
        val lineStart = IjVimEditor(editor).getLineStartForOffset(caret.offset)
        if (caret.offset == lineEnd && lineEnd != lineStart) caret.moveToInlayAwareOffset(caret.offset - 1)
      }
    }
    return
  }
  editor.caretModel.allCarets.forEach { caret ->
    if (caret.hasSelection() && caret.selectionEnd == caret.offset) {
      if (caret.selectionEnd <= 0) return@forEach
      if (IjVimEditor(editor).getLineStartForOffset(caret.selectionEnd - 1) != caret.selectionEnd - 1 &&
        caret.selectionEnd > 1 && editor.document.text[caret.selectionEnd - 1] == '\n'
      ) {
        caret.moveToInlayAwareOffset(caret.selectionEnd - 2)
      } else {
        caret.moveToInlayAwareOffset(caret.selectionEnd - 1)
      }
    }
  }
}

@Deprecated("Use same method on VimCaret")
internal fun Caret.vimSetSelection(start: Int, end: Int = start, moveCaretToSelectionEnd: Boolean = false) {
  vimSelectionStart = start
  setVisualSelection(start, end, this.vim)
  if (moveCaretToSelectionEnd && !editor.vim.inBlockSelection) moveToInlayAwareOffset(end)
}
