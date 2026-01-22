/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.fold

import com.maddyhome.idea.vim.api.Key
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getOrPutWindowData
import com.maddyhome.idea.vim.api.injector

object FoldState {

  private val foldLevelKey = Key<Int>("foldlevel")

  fun getFoldLevel(editor: VimEditor): Int {
    return injector.vimStorageService.getOrPutWindowData(editor, foldLevelKey) {
      editor.getMaxFoldDepth()
    }
  }

  fun setFoldLevel(editor: VimEditor, level: Int) {
    val maxDepth = editor.getMaxFoldDepth()
    val coercedLevel = level.coerceIn(0, maxDepth + 1)

    injector.vimStorageService.putDataToWindow(editor, foldLevelKey, coercedLevel)
    editor.applyFoldLevel(coercedLevel)
  }

  fun closeAllFolds(editor: VimEditor) {
    setFoldLevel(editor, 0)
  }

  fun openAllFolds(editor: VimEditor) {
    val maxDepth = editor.getMaxFoldDepth()
    setFoldLevel(editor, maxDepth + 1)
  }
}
