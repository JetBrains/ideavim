/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model

import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType

sealed class ExecutionResult {

  object Success : ExecutionResult()
  object Error : ExecutionResult()

  object Break : ExecutionResult()
  object Continue : ExecutionResult()
  class Return(val value: VimDataType) : ExecutionResult()
}
