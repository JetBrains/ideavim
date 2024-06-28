/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.key

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.state.mode.SelectionType

/**
 * @author vlan
 */
interface OperatorFunction {
  /**
   * The value of 'operatorfunc' to be used as the operator function in 'g@'.
   *
   *
   * Make sure to synchronize your function properly using read/write actions.
   */
  fun apply(editor: VimEditor, context: ExecutionContext, selectionType: SelectionType?): Boolean
}
