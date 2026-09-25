/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.common.TextRange

interface VimPsiService {
  /**
   * @return triple of comment range, comment prefix, comment suffix or null if there is no comment at the given position
   */
  fun getCommentAtPos(editor: VimEditor, pos: Int): Pair<TextRange, Pair<String, String>?>?

  /**
   * @param isInner A flag indicating whether the start and end quote characters should be considered part of the string:
   *                - If set to true, only the text between the quote characters is included in the range.
   *                - If set to false, the quote characters at the boundaries are included as part of the string range.
   *
   * NOTE: Regardless of the [isInner] value, a TextRange will be returned if the caret is positioned on a quote character.
   */
  fun getDoubleQuotedString(editor: VimEditor, pos: Int, isInner: Boolean): TextRange?

  /**
   * @param isInner A flag indicating whether the start and end quote characters should be considered part of the string:
   *                - If set to true, only the text between the quote characters is included in the range.
   *                - If set to false, the quote characters at the boundaries are included as part of the string range.
   *
   * NOTE: Regardless of the [isInner] value, a TextRange will be returned if the caret is positioned on a quote character.
   */
  fun getSingleQuotedString(editor: VimEditor, pos: Int, isInner: Boolean): TextRange?

  /**
   * Finds the range of contiguous comment lines around the given [cursorLine].
   * Walks up and down from the cursor, checking each line for comment-only content via PSI.
   *
   * @return a [TextRange] covering the full comment block (line-wise), or null if the cursor is not on a comment line.
   */
  fun getCommentBlockRange(editor: VimEditor, cursorLine: Int): TextRange? = null

  /**
   * Whether the language of the file can indent a new line on its own.
   *
   * This is the counterpart of Vim's ['indentexpr'] and ['cindent'] - an indent that is computed from the syntax of the
   * language rather than copied from the previous line. A file the IDE has no formatter for can only copy, which is
   * what ['autoindent'] does, so the option applies to such a file and not to one that indents by itself.
   */
  fun hasLanguageIndentSupport(editor: VimEditor): Boolean = false
}
