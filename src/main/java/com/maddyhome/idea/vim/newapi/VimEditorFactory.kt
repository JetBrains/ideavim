/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor

interface VimEditorFactory {
  fun createVimEditor(editor: Editor): VimEditor
  fun extractEditor(vimEditor: VimEditor): Editor
  fun createVimCaret(caret: Caret): VimCaret
  fun extractCaret(vimCaret: ImmutableVimCaret): Caret

  companion object {
    @JvmStatic
    fun getInstance(): VimEditorFactory = service()
  }
}
