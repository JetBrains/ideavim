/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi

import com.intellij.vim.api.Color
import com.intellij.vim.api.Highlighter
import com.maddyhome.idea.vim.api.VimEditor

interface VimHighlightingService {
  fun addHighlighter(
    editor: VimEditor,
    startOffset: Int,
    endOffset: Int,
    backgroundColor: Color?,
    foregroundColor: Color?,
  ): Highlighter

  fun removeHighlighter(editor: VimEditor, highlighter: Highlighter)
}