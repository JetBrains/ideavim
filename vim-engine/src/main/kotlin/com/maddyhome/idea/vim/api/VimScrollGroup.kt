/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

interface VimScrollGroup {
  fun scrollCaretIntoView(editor: VimEditor)
  fun scrollFullPage(editor: VimEditor, caret: VimCaret, pages: Int): Boolean
  fun scrollHalfPage(editor: VimEditor, caret: VimCaret, rawCount: Int, down: Boolean): Boolean
  fun scrollLines(editor: VimEditor, lines: Int): Boolean
  fun scrollCurrentLineToDisplayTop(editor: VimEditor, rawCount: Int, start: Boolean): Boolean
  fun scrollCurrentLineToDisplayMiddle(editor: VimEditor, rawCount: Int, start: Boolean): Boolean
  fun scrollCurrentLineToDisplayBottom(editor: VimEditor, rawCount: Int, start: Boolean): Boolean
  fun scrollColumns(editor: VimEditor, columns: Int): Boolean
  fun scrollCaretColumnToDisplayLeftEdge(vimEditor: VimEditor): Boolean
  fun scrollCaretColumnToDisplayRightEdge(editor: VimEditor): Boolean
}
