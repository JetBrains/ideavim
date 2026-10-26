/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ui.ex

import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.api.CommandLineType
import com.maddyhome.idea.vim.common.Direction

/** 'incsearch' for `/` and `?`. The whole command line is the pattern */
internal class SearchPreview(editor: Editor, private val type: CommandLineType.Search) : CommandLinePreview {
  private val incsearch = IncsearchSession(editor)
  private val forwards = type.direction == Direction.FORWARDS

  override val matchOffset: Int
    get() = incsearch.matchOffset

  override fun update(editor: Editor, text: String) {
    incsearch.restartFromFirstMatch()
    show(editor, text)
  }

  override fun advanceMatch(editor: Editor, text: String, next: Boolean) {
    incsearch.advanceMatch(next)
    show(editor, text)
  }

  override fun close(editor: Editor?, resetCaret: Boolean) {
    incsearch.close(editor, resetCaret)
  }

  private fun show(editor: Editor, text: String) {
    incsearch.showSearch(editor, text, type.cmdType, forwards)
  }
}
