/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.parser.error

import com.maddyhome.idea.vim.regexp.VimRegexErrors

internal data class VimRegexParserException(
  val errorCode: VimRegexErrors,
) : RuntimeException(errorCode.toString())