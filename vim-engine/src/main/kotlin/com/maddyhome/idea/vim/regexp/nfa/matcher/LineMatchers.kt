/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.nfa.matcher

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.regexp.match.VimMatchGroupCollection

internal class AtLineMatcher(private val lineNumber: Int) : Matcher {
  override fun matches(
    editor: VimEditor,
    index: Int,
    groups: VimMatchGroupCollection,
    isCaseInsensitive: Boolean,
  ): MatcherResult {
    return if (editor.offsetToBufferPosition(index).line + 1 == lineNumber) MatcherResult.Success(0)
    else MatcherResult.Failure
  }
}

internal class BeforeLineMatcher(private val lineNumber: Int) : Matcher {
  override fun matches(
    editor: VimEditor,
    index: Int,
    groups: VimMatchGroupCollection,
    isCaseInsensitive: Boolean,
  ): MatcherResult {
    return if (editor.offsetToBufferPosition(index).line + 1 < lineNumber) MatcherResult.Success(0)
    else MatcherResult.Failure
  }
}

internal class AfterLineMatcher(private val lineNumber: Int) : Matcher {
  override fun matches(
    editor: VimEditor,
    index: Int,
    groups: VimMatchGroupCollection,
    isCaseInsensitive: Boolean,
  ): MatcherResult {
    return if (editor.offsetToBufferPosition(index).line + 1 > lineNumber) MatcherResult.Success(0)
    else MatcherResult.Failure
  }
}
internal class AtLineCursorMatcher : Matcher {
  override fun matches(
    editor: VimEditor,
    index: Int,
    groups: VimMatchGroupCollection,
    isCaseInsensitive: Boolean,
  ): MatcherResult {
    return if (editor.offsetToBufferPosition(index).line == editor.offsetToBufferPosition(editor.currentCaret().offset.point).line) MatcherResult.Success(0)
    else MatcherResult.Failure
  }
}

internal class BeforeLineCursorMatcher : Matcher {
  override fun matches(
    editor: VimEditor,
    index: Int,
    groups: VimMatchGroupCollection,
    isCaseInsensitive: Boolean,
  ): MatcherResult {
    return if (editor.offsetToBufferPosition(index).line < editor.offsetToBufferPosition(editor.currentCaret().offset.point).line) MatcherResult.Success(0)
    else MatcherResult.Failure
  }
}

internal class AfterLineCursorMatcher : Matcher {
  override fun matches(
    editor: VimEditor,
    index: Int,
    groups: VimMatchGroupCollection,
    isCaseInsensitive: Boolean,
  ): MatcherResult {
    return if (editor.offsetToBufferPosition(index).line > editor.offsetToBufferPosition(editor.currentCaret().offset.point).line) MatcherResult.Success(0)
    else MatcherResult.Failure
  }
}
