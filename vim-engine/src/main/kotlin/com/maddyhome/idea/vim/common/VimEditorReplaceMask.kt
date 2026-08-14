/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.common

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getLineEndForOffset
import com.maddyhome.idea.vim.api.injector

sealed interface ReplaceModeEdit {
  data class Overwrote(val original: Char) : ReplaceModeEdit

  data object Inserted : ReplaceModeEdit
}

/**
 * Vim's replace stack, keyed by the offset of the typed character rather than by insertion order.
 */
class VimEditorReplaceMask(private val editor: VimEditor) {
  private val edits = mutableMapOf<LiveRange, ReplaceModeEdit>()

  fun recordTypedCharacterAtCaret() {
    for (caret in editor.carets()) {
      val offset = caret.offset
      if (offset < editor.getLineEndForOffset(offset)) {
        record(offset, ReplaceModeEdit.Overwrote(editor.charAt(offset)))
      } else {
        record(offset, ReplaceModeEdit.Inserted)
      }
    }
  }

  fun recordLineBreakAtCaret() {
    for (caret in editor.carets()) {
      recordAutoIndentAndLineBreakBefore(caret.offset)
    }
  }

  private fun recordAutoIndentAndLineBreakBefore(caretOffset: Int) {
    var offset = caretOffset - 1
    while (offset >= 0 && isAutoIndentWhitespace(offset)) {
      record(offset, ReplaceModeEdit.Inserted)
      offset--
    }
    if (offset >= 0 && editor.charAt(offset) == '\n') {
      record(offset, ReplaceModeEdit.Inserted)
    }
  }

  private fun isAutoIndentWhitespace(offset: Int): Boolean {
    val char = editor.charAt(offset)
    return char != '\n' && char.isWhitespace()
  }

  fun popEditAt(offset: Int): ReplaceModeEdit? {
    return edits.remove(markerAt(offset))
  }

  private fun record(offset: Int, edit: ReplaceModeEdit) {
    edits[markerAt(offset)] = edit
  }

  private fun markerAt(offset: Int): LiveRange = editor.createLiveMarker(offset, offset)
}

fun forgetAllReplaceMasks() {
  injector.editorGroup.getEditors().forEach { it.replaceMask = null }
}
