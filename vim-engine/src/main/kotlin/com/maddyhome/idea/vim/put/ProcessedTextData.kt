/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.put

import com.maddyhome.idea.vim.common.VimCopiedText
import com.maddyhome.idea.vim.state.mode.SelectionType

data class ProcessedTextData(
  val registerChar: Char?,
  val copiedText: VimCopiedText,
  val typeInRegister: SelectionType,
)
