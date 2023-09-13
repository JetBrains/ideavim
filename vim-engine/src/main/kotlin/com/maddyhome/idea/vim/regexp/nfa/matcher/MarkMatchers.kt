/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.nfa.matcher

import com.maddyhome.idea.vim.api.BufferPosition
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.regexp.match.VimMatchGroupCollection

internal class AtMarkMatcher(val mark: Char) : Matcher {
  override fun matches(
    editor: VimEditor,
    index: Int,
    groups: VimMatchGroupCollection,
    isCaseInsensitive: Boolean,
  ): MatcherResult {
    val markIndex = editor.currentCaret().markStorage.getMark(mark)?.let {
      editor.bufferPositionToOffset(BufferPosition(it.line, it.col))
    }?:run { return MatcherResult.Failure }
    return if (index == markIndex) MatcherResult.Success(0)
    else MatcherResult.Failure
  }
}

internal class BeforeMarkMatcher(val mark: Char) : Matcher {
  override fun matches(
    editor: VimEditor,
    index: Int,
    groups: VimMatchGroupCollection,
    isCaseInsensitive: Boolean,
  ): MatcherResult {
    val markIndex = editor.currentCaret().markStorage.getMark(mark)?.let {
      editor.bufferPositionToOffset(BufferPosition(it.line, it.col))
    }?:run { return MatcherResult.Failure }
    return if (index < markIndex) MatcherResult.Success(0)
    else MatcherResult.Failure
  }
}

internal class AfterMarkMatcher(val mark: Char) : Matcher {
  override fun matches(
    editor: VimEditor,
    index: Int,
    groups: VimMatchGroupCollection,
    isCaseInsensitive: Boolean,
  ): MatcherResult {
    val markIndex = editor.currentCaret().markStorage.getMark(mark)?.let {
      editor.bufferPositionToOffset(BufferPosition(it.line, it.col))
    }?:run { return MatcherResult.Failure }
    return if (index > markIndex) MatcherResult.Success(0)
    else MatcherResult.Failure
  }
}