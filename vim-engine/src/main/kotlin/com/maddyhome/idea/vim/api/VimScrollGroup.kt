/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

interface VimScrollGroup {
  fun scrollCaretIntoView(editor: VimEditor)

  /**
   * @param caretChangedLine True if the caret has just moved to a different visual line. With a large 'scrolloff', a
   * line that can't be put exactly in the middle of the screen still scrolls on every move, but not while typing
   */
  fun scrollCaretIntoView(editor: VimEditor, caretChangedLine: Boolean) = scrollCaretIntoView(editor)

  fun scrollFullPage(editor: VimEditor, caret: VimCaret, pages: Int): Boolean
  fun scrollHalfPage(editor: VimEditor, caret: VimCaret, rawCount: Int, down: Boolean): Boolean
  fun scrollLines(editor: VimEditor, lines: Int): Boolean
  fun scrollCurrentLineToDisplayTop(editor: VimEditor, rawCount: Int, start: Boolean): Boolean
  fun scrollCurrentLineToDisplayMiddle(editor: VimEditor, rawCount: Int, start: Boolean): Boolean
  fun scrollCurrentLineToDisplayBottom(editor: VimEditor, rawCount: Int, start: Boolean): Boolean
  fun scrollColumns(editor: VimEditor, columns: Int): Boolean
  fun scrollCaretColumnToDisplayLeftEdge(vimEditor: VimEditor): Boolean
  fun scrollCaretColumnToDisplayRightEdge(editor: VimEditor): Boolean

  /**
   * Called when scroll-related options (e.g. 'scrolloff') change for an editor.
   * Implementations should ensure the caret is still visible with the new option values.
   */
  fun onScrollOptionChanged(editor: VimEditor) {}
}
