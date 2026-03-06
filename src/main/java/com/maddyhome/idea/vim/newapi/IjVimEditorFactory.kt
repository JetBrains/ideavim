/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor

class IjVimEditorFactory : VimEditorFactory {
  override fun createVimEditor(editor: Editor): VimEditor = IjVimEditor(editor)
  override fun extractEditor(vimEditor: VimEditor): Editor = (vimEditor as IjVimEditor).editor
  override fun createVimCaret(caret: Caret): VimCaret = IjVimCaret(caret)
  override fun extractCaret(vimCaret: ImmutableVimCaret): Caret = (vimCaret as IjVimCaret).caret
}
