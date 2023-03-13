/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

public interface VimRegexpService {
  public fun matches(pattern: String, text: String?, ignoreCase: Boolean = false): Boolean
  public fun getAllMatches(text: String, pattern: String): List<Pair<Int, Int>>
}
