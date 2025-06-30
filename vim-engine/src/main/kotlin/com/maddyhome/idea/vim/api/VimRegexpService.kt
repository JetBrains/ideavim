/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

interface VimRegexpService {
  fun matches(pattern: String, text: String?, ignoreCase: Boolean = false): Boolean
  fun getAllMatches(text: String, pattern: String): List<Pair<Int, Int>>
  fun findNext(pattern: String, text: String, start: Int, includeStartPosition: Boolean): Pair<Int, Int>?
  fun findPrevious(pattern: String, text: String, start: Int): Pair<Int, Int>?
}
