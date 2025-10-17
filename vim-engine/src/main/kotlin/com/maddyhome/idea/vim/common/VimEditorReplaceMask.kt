/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.common

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector

class VimEditorReplaceMask {
  private val changedChars = mutableMapOf<LiveRange, Char>()

  fun recordChangeAtCaret(editor: VimEditor) {
    for (caret in editor.carets()) {
      val offset = caret.offset
      if (offset < editor.fileSize()) {
        val marker = editor.createLiveMarker(offset, offset)
        changedChars[marker] = editor.charAt(offset)
      }
    }
  }

  fun popChange(editor: VimEditor, offset: Int): Char? {
    val marker = editor.createLiveMarker(offset, offset)
    val change = changedChars[marker]
    changedChars.remove(marker)
    return change
  }
}

fun forgetAllReplaceMasks() {
  injector.editorGroup.getEditors().forEach { it.replaceMask = null }
}