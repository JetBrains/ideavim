/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.nfa

internal sealed class MultiDelimiter {
  data class IntMultiDelimiter(val i: Int) : MultiDelimiter()
  object InfiniteMultiDelimiter : MultiDelimiter()
}