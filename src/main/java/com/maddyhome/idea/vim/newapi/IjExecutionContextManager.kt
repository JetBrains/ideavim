/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.actionSystem.CaretSpecificDataContext
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ExecutionContextManagerBase
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.helper.EditorDataContext

@Service
internal class IjExecutionContextManager : ExecutionContextManagerBase() {
  override fun onEditor(editor: VimEditor, prevContext: ExecutionContext?): ExecutionContext.Editor {
    if (prevContext is ExecutionContext.CaretAndEditor) {
      return prevContext
    }
    return IjEditorExecutionContext(EditorDataContext.init((editor as IjVimEditor).editor, prevContext?.ij))
  }

  override fun onCaret(caret: VimCaret, prevContext: ExecutionContext.Editor): ExecutionContext.CaretAndEditor {
    return IjCaretAndEditorExecutionContext(CaretSpecificDataContext.create(prevContext.ij, caret.ij))
  }
}
