/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.key.VimKeyStroke

abstract class VimModalInputBase : VimModalInput {
  override fun handleKey(key: VimKeyStroke, editor: VimEditor, executionContext: ExecutionContext) {
    inputInterceptor.consumeKey(key, editor, executionContext)
  }
}