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
 * Matcher used to match against a previously captured group
 *
 * @param groupNumber The number of the back-referenced captured group
 */
internal class BackreferenceMatcher(private val groupNumber: Int) : Matcher {
  override fun matches(
    editor: VimEditor,
    index: Int,
    groups: VimMatchGroupCollection,
    isCaseInsensitive: Boolean,
    possibleCursors: MutableList<VimCaret>,
  ): MatcherResult {
    val capturedGroup = groups.get(groupNumber) ?: run {
      // TODO: throw illegal backreference error
      return MatcherResult.Failure
    }
    val capturedString = if (isCaseInsensitive) capturedGroup.value.lowercase()
    else capturedGroup.value

    if (editor.text().length - index < capturedString.length) return MatcherResult.Failure

    val editorString =
      if (isCaseInsensitive) editor.text().substring(index until index + capturedString.length).lowercase()
      else editor.text().substring(index until index + capturedString.length)

    return if (capturedString == editorString)
      MatcherResult.Success(capturedString.length)
    else
      MatcherResult.Failure
  }

  override fun isEpsilon(): Boolean {
    return false
  }
}