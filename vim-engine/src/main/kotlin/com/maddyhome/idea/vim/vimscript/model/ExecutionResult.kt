/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model

import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType

public sealed class ExecutionResult {

  public object Success : ExecutionResult()
  public object Error : ExecutionResult()

  public object Break : ExecutionResult()
  public object Continue : ExecutionResult()
  public class Return(public val value: VimDataType) : ExecutionResult()
}
