/*
 * Copyright 2003-2024 The IdeaVim authors
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
}