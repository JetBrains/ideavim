/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.visual

import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.api.getLineEndForOffset
import com.maddyhome.idea.vim.api.getLineStartForOffset
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.helper.isEndAllowed
import com.maddyhome.idea.vim.helper.moveToInlayAwareOffset
import com.maddyhome.idea.vim.newapi.IjVimEditor

fun moveCaretOneCharLeftFromSelectionEnd(editor: Editor, predictedMode: VimStateMachine.Mode) {
  if (predictedMode != VimStateMachine.Mode.VISUAL) {
    if (!predictedMode.isEndAllowed) {
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
