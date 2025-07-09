/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.key.interceptors

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import javax.swing.KeyStroke

/**
 * Modal key interceptor
 * It receives keys until it can build an input and handle it
 */
interface VimInputInterceptor {
  fun consumeKey(
    key: KeyStroke,
    editor: VimEditor,
    context: ExecutionContext,
  )
}
