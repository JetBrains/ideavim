/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

public interface VimScrollGroup {
  public fun scrollCaretIntoView(editor: VimEditor)
  public fun scrollFullPage(editor: VimEditor, caret: VimCaret, pages: Int): Boolean
  public fun scrollHalfPage(editor: VimEditor, caret: VimCaret, rawCount: Int, down: Boolean): Boolean
  public fun scrollLines(editor: VimEditor, lines: Int): Boolean
  public fun scrollCurrentLineToDisplayTop(editor: VimEditor, rawCount: Int, start: Boolean): Boolean
  public fun scrollCurrentLineToDisplayMiddle(editor: VimEditor, rawCount: Int, start: Boolean): Boolean
  public fun scrollCurrentLineToDisplayBottom(editor: VimEditor, rawCount: Int, start: Boolean): Boolean
  public fun scrollColumns(editor: VimEditor, columns: Int): Boolean
  public fun scrollCaretColumnToDisplayLeftEdge(vimEditor: VimEditor): Boolean
  public fun scrollCaretColumnToDisplayRightEdge(editor: VimEditor): Boolean
}
