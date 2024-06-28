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
import com.maddyhome.idea.vim.state.KeyHandlerState

interface MappingInfoLayer {
  fun getPresentableString(): String
  fun execute(editor: VimEditor, context: ExecutionContext, keyState: KeyHandlerState)
}
