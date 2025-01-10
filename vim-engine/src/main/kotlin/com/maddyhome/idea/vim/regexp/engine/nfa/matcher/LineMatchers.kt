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

internal class AtLineMatcher(private val lineNumber: Int) : Matcher {
  override fun matches(
    editor: VimEditor,
    index: Int,
    groups: VimMatchGroupCollection,
    isCaseInsensitive: Boolean,
    possibleCursors: MutableList<VimCaret>,
  ): MatcherResult {
    return if (editor.offsetToBufferPosition(index).line + 1 == lineNumber) MatcherResult.Success(0)
    else MatcherResult.Failure
  }

  override fun isEpsilon(): Boolean {
    return true
  }
}

internal class BeforeLineMatcher(private val lineNumber: Int) : Matcher {
  override fun matches(
    editor: VimEditor,
    index: Int,
    groups: VimMatchGroupCollection,
    isCaseInsensitive: Boolean,
    possibleCursors: MutableList<VimCaret>,
  ): MatcherResult {
    return if (editor.offsetToBufferPosition(index).line + 1 < lineNumber) MatcherResult.Success(0)
    else MatcherResult.Failure
  }

  override fun isEpsilon(): Boolean {
    return true
  }
}

internal class AfterLineMatcher(private val lineNumber: Int) : Matcher {
  override fun matches(
    editor: VimEditor,
    index: Int,
    groups: VimMatchGroupCollection,
    isCaseInsensitive: Boolean,
    possibleCursors: MutableList<VimCaret>,
  ): MatcherResult {
    return if (editor.offsetToBufferPosition(index).line + 1 > lineNumber) MatcherResult.Success(0)
    else MatcherResult.Failure
  }

  override fun isEpsilon(): Boolean {
    return true
  }
}

internal class AtLineCursorMatcher : Matcher {
  override fun matches(
    editor: VimEditor,
    index: Int,
    groups: VimMatchGroupCollection,
    isCaseInsensitive: Boolean,
    possibleCursors: MutableList<VimCaret>,
  ): MatcherResult {
    return if (possibleCursors.any { editor.offsetToBufferPosition(index).line == editor.offsetToBufferPosition(it.offset).line }) {
      val newPossibleCursors =
        possibleCursors.filter { editor.offsetToBufferPosition(index).line == editor.offsetToBufferPosition(it.offset).line }
      possibleCursors.clear()
      possibleCursors.addAll(newPossibleCursors)
      MatcherResult.Success(0)
    } else MatcherResult.Failure
  }

  override fun isEpsilon(): Boolean {
    return true
  }
}

internal class BeforeLineCursorMatcher : Matcher {
  override fun matches(
    editor: VimEditor,
    index: Int,
    groups: VimMatchGroupCollection,
    isCaseInsensitive: Boolean,
    possibleCursors: MutableList<VimCaret>,
  ): MatcherResult {
    return if (possibleCursors.any { editor.offsetToBufferPosition(index).line < editor.offsetToBufferPosition(it.offset).line }) {
      val newPossibleCursors =
        possibleCursors.filter { editor.offsetToBufferPosition(index).line < editor.offsetToBufferPosition(it.offset).line }
      possibleCursors.clear()
      possibleCursors.addAll(newPossibleCursors)
      MatcherResult.Success(0)
    } else MatcherResult.Failure
  }

  override fun isEpsilon(): Boolean {
    return true
  }
}

internal class AfterLineCursorMatcher : Matcher {
  override fun matches(
    editor: VimEditor,
    index: Int,
    groups: VimMatchGroupCollection,
    isCaseInsensitive: Boolean,
    possibleCursors: MutableList<VimCaret>,
  ): MatcherResult {
    return if (possibleCursors.any { editor.offsetToBufferPosition(index).line > editor.offsetToBufferPosition(it.offset).line }) {
      val newPossibleCursors =
        possibleCursors.filter { editor.offsetToBufferPosition(index).line > editor.offsetToBufferPosition(it.offset).line }
      possibleCursors.clear()
      possibleCursors.addAll(newPossibleCursors)
      MatcherResult.Success(0)
    } else MatcherResult.Failure
  }

  override fun isEpsilon(): Boolean {
    return true
  }
}
