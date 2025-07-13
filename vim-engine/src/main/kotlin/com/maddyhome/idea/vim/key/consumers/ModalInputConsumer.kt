/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.key.consumers

import com.maddyhome.idea.vim.KeyProcessResult
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.key.KeyConsumer
import com.maddyhome.idea.vim.key.VimKeyStroke

internal class ModalInputConsumer : KeyConsumer {
  override fun consumeKey(
    key: VimKeyStroke,
    editor: VimEditor,
    allowKeyMappings: Boolean,
    keyProcessResultBuilder: KeyProcessResult.KeyProcessResultBuilder,
  ): Boolean {
    val modalInput = injector.modalInput.getCurrentModalInput() ?: return false
    keyProcessResultBuilder.addExecutionStep { _, lambdaVimEditor, lambdaExecutionContext ->
      modalInput.inputInterceptor.consumeKey(key, lambdaVimEditor, lambdaExecutionContext)
    }
    return true
  }
}
