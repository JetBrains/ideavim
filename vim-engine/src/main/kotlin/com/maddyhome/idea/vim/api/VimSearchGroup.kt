/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.common.Direction
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.ex.ranges.LineRange
import com.maddyhome.idea.vim.regexp.CharPointer
import com.maddyhome.idea.vim.vimscript.model.VimLContext

public interface VimSearchGroup {
  public var lastSearchPattern: String?
  public var lastSubstitutePattern: String?
  public fun findUnderCaret(editor: VimEditor): TextRange?
  public fun searchBackward(editor: VimEditor, offset: Int, count: Int): TextRange?
  public fun getNextSearchRange(editor: VimEditor, count: Int, forwards: Boolean): TextRange?
  public fun processSearchRange(
    editor: VimEditor,
    pattern: String,
    patternOffset: Int,
    startOffset: Int,
    direction: Direction,
  ): Int

  public fun searchNext(editor: VimEditor, caret: ImmutableVimCaret, count: Int): Int
  public fun searchPrevious(editor: VimEditor, caret: ImmutableVimCaret, count: Int): Int
  public fun processSearchCommand(editor: VimEditor, command: String, startOffset: Int, dir: Direction): Int
  public fun searchWord(editor: VimEditor, caret: ImmutableVimCaret, count: Int, whole: Boolean, dir: Direction): Int
  public fun processSubstituteCommand(
    editor: VimEditor,
    caret: VimCaret,
    range: LineRange,
    excmd: String,
    exarg: String,
    parent: VimLContext,
  ): Boolean
  // TODO rewrite this
  public fun search_regcomp(pat: CharPointer?, which_pat: Int, patSave: Int): Pair<Boolean, Triple<Any, String, Any>>
  public fun findDecimalNumber(line: String): Int?
  public fun clearSearchHighlight()

  public fun getLastSearchDirection(): Direction
  // Matching the values defined in Vim. Do not change these values, they are used as indexes
  public companion object {
    public val RE_SEARCH: Int = 0 // Save/use search pattern

    public val RE_SUBST: Int = 1 // Save/use substitute pattern

    public val RE_BOTH: Int = 2 // Save to both patterns

    public val RE_LAST: Int = 2 // Use last used pattern if "pat" is NULL
  }
}
