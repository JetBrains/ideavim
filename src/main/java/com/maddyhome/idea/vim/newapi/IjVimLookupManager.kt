/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.IdeLookup
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimLookupManager

@Service
internal class IjVimLookupManager : VimLookupManager {
  override fun getActiveLookup(editor: VimEditor): IjLookup? {
    return LookupManager.getActiveLookup(editor.ij)?.let { IjLookup(it) }
  }
}

internal class IjLookup(val lookup: Lookup) : IdeLookup {
  override fun down(caret: ImmutableVimCaret, context: ExecutionContext) {
    EditorActionManager.getInstance().getActionHandler(IdeActions.ACTION_EDITOR_MOVE_CARET_DOWN)
      .execute(caret.editor.ij, caret.ij, context.ij)
  }

  override fun up(caret: ImmutableVimCaret, context: ExecutionContext) {
    EditorActionManager.getInstance().getActionHandler(IdeActions.ACTION_EDITOR_MOVE_CARET_UP)
      .execute(caret.editor.ij, caret.ij, context.ij)
  }
}
