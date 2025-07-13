/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.key

import com.maddyhome.idea.vim.KeyProcessResult
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.key.VimKeyStroke

internal interface KeyConsumer {
  /**
   * @return true if consumed key and could do something meaningful wit it
   */
  fun consumeKey(
    key: VimKeyStroke,
    editor: VimEditor,
    allowKeyMappings: Boolean,
    keyProcessResultBuilder: KeyProcessResult.KeyProcessResultBuilder,
  ): Boolean
}
