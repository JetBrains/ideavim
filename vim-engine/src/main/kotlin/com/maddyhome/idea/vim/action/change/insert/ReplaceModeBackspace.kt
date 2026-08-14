/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.change.insert

import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.ReplaceModeEdit

/**
 * Undoes the character typed at [offset] in Replace mode, then moves the caret onto it.
 */
internal fun replaceModeBackspace(editor: VimEditor, caret: VimCaret, offset: Int) {
  val undoneEdit = editor.replaceMask?.popEditAt(offset)
  if (undoneEdit != null) {
    replaceTypedCharacter(editor, caret, offset, undoneEdit.textBeforeEdit)
  }
  caret.moveToOffset(offset)
}

private fun replaceTypedCharacter(editor: VimEditor, caret: VimCaret, offset: Int, text: String) {
  injector.changeGroup.replaceText(editor, caret, offset, offset + 1, text)
}

private val ReplaceModeEdit.textBeforeEdit: String
  get() = when (this) {
    is ReplaceModeEdit.Overwrote -> original.toString()
    ReplaceModeEdit.Inserted -> ""
  }
