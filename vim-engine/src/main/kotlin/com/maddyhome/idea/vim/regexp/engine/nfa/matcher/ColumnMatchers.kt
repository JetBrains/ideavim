/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.engine.nfa.matcher

import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.regexp.match.VimMatchGroupCollection

internal class AtColumnMatcher(private val columnNumber: Int) : Matcher {
  override fun matches(
    editor: VimEditor,
    index: Int,
    groups: VimMatchGroupCollection,
    isCaseInsensitive: Boolean,
    possibleCursors: MutableList<VimCaret>,
  ): MatcherResult {
    return if (editor.offsetToBufferPosition(index).column + 1 == columnNumber) MatcherResult.Success(0)
    else MatcherResult.Failure
  }

  override fun isEpsilon(): Boolean {
    return true
  }
}

internal class BeforeColumnMatcher(private val columnNumber: Int) : Matcher {
  override fun matches(
    editor: VimEditor,
    index: Int,
    groups: VimMatchGroupCollection,
    isCaseInsensitive: Boolean,
    possibleCursors: MutableList<VimCaret>,
  ): MatcherResult {
    return if (editor.offsetToBufferPosition(index).column + 1 < columnNumber) MatcherResult.Success(0)
    else MatcherResult.Failure
  }

  override fun isEpsilon(): Boolean {
    return true
  }
}

internal class AfterColumnMatcher(private val columnNumber: Int) : Matcher {
  override fun matches(
    editor: VimEditor,
    index: Int,
    groups: VimMatchGroupCollection,
    isCaseInsensitive: Boolean,
    possibleCursors: MutableList<VimCaret>,
  ): MatcherResult {
    return if (editor.offsetToBufferPosition(index).column + 1 > columnNumber) MatcherResult.Success(0)
    else MatcherResult.Failure
  }

  override fun isEpsilon(): Boolean {
    return true
  }
}

internal class AtColumnCursorMatcher : Matcher {
  override fun matches(
    editor: VimEditor,
    index: Int,
    groups: VimMatchGroupCollection,
    isCaseInsensitive: Boolean,
    possibleCursors: MutableList<VimCaret>,
  ): MatcherResult {
    return if (possibleCursors.any { editor.offsetToBufferPosition(index).column == editor.offsetToBufferPosition(it.offset).column }) {
      val newPossibleCursors =
        possibleCursors.filter { editor.offsetToBufferPosition(index).column == editor.offsetToBufferPosition(it.offset).column }
      possibleCursors.clear()
      possibleCursors.addAll(newPossibleCursors)
      MatcherResult.Success(0)
    } else MatcherResult.Failure
  }

  override fun isEpsilon(): Boolean {
    return true
  }
}

internal class BeforeColumnCursorMatcher : Matcher {
  override fun matches(
    editor: VimEditor,
    index: Int,
    groups: VimMatchGroupCollection,
    isCaseInsensitive: Boolean,
    possibleCursors: MutableList<VimCaret>,
  ): MatcherResult {
    return if (possibleCursors.any { editor.offsetToBufferPosition(index).column < editor.offsetToBufferPosition(it.offset).column }) {
      val newPossibleCursors =
        possibleCursors.filter { editor.offsetToBufferPosition(index).column < editor.offsetToBufferPosition(it.offset).column }
      possibleCursors.clear()
      possibleCursors.addAll(newPossibleCursors)
      MatcherResult.Success(0)
    } else MatcherResult.Failure
  }

  override fun isEpsilon(): Boolean {
    return true
  }
}

internal class AfterColumnCursorMatcher : Matcher {
  override fun matches(
    editor: VimEditor,
    index: Int,
    groups: VimMatchGroupCollection,
    isCaseInsensitive: Boolean,
    possibleCursors: MutableList<VimCaret>,
  ): MatcherResult {
    return if (possibleCursors.any { editor.offsetToBufferPosition(index).column > editor.offsetToBufferPosition(it.offset).column }) {
      val newPossibleCursors =
        possibleCursors.filter { editor.offsetToBufferPosition(index).column > editor.offsetToBufferPosition(it.offset).column }
      possibleCursors.clear()
      possibleCursors.addAll(newPossibleCursors)
      MatcherResult.Success(0)
    } else MatcherResult.Failure
  }

  override fun isEpsilon(): Boolean {
    return true
  }
}
