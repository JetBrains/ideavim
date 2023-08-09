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

internal class BackreferenceMatcher(val groupNumber: Int) : Matcher {
  override fun matches(editor: VimEditor, index: Int, groups: VimMatchGroupCollection): MatcherResult {
    if (groups.get(groupNumber) == null) {
      // TODO: throw illegal backreference error
      return MatcherResult.Failure
    }
    val capturedString = groups.get(groupNumber)!!.value

    if (editor.text().length - index < capturedString.length) return MatcherResult.Failure
    return if (editor.text().substring(index until index + capturedString.length) == capturedString)
      MatcherResult.Success(capturedString.length)
    else
      MatcherResult.Failure
  }
}