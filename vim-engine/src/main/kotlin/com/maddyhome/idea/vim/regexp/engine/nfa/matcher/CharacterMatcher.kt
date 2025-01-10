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

/**
 * Matcher used to match against single characters
 */
internal class CharacterMatcher(val char: Char) : Matcher {
  override fun matches(
    editor: VimEditor,
    index: Int,
    groups:
    VimMatchGroupCollection,
    isCaseInsensitive: Boolean,
    possibleCursors: MutableList<VimCaret>,
  ): MatcherResult {
    // Special case. Vim always has at least one line in a buffer, so always matches newline with the end of the buffer.
    // You can see this in action by opening a new (empty) file and typing `:%s/\n/foo\r/`. Suddenly, your empty file
    // now contains two lines, and we've matched a '\n' that wasn't in the text.
    // (Diving deeper: we have to use '\r' in the substitution pattern because '\n' is converted to NULL. And AIUI, when
    // matching regexes, each line is delimited by NULL, so when comparing the current char with a newline, it actually
    // compares with '\0', so obviously matches newlines and the end of file)
    val length = editor.text().length
    if (index == length && char == '\n') return MatcherResult.Success(0) // Nothing left to consume
    if (index >= length) return MatcherResult.Failure

    val targetChar = if (isCaseInsensitive) char.lowercaseChar() else char
    val editorChar = if (isCaseInsensitive) editor.text()[index].lowercaseChar() else editor.text()[index]

    return if (targetChar == editorChar) MatcherResult.Success(1)
    else MatcherResult.Failure
  }

  override fun isEpsilon(): Boolean {
    return false
  }
}
