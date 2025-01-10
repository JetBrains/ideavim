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
import com.maddyhome.idea.vim.api.injector
import javax.swing.KeyStroke

// TODO make it safer to utilise key.isCloseKeystroke and similar logic
abstract class VimInputInterceptorBase<T> : VimInputInterceptor<T> {
  override fun consumeKey(
    key: KeyStroke,
    editor: VimEditor,
    context: ExecutionContext,
  ) {
    val completeInput = buildInput(key) ?: return
    executeInput(completeInput, editor, context)
  }

  protected open fun onFinish() {
    closeModalInputPrompt()
  }

  protected open fun closeModalInputPrompt() {
    val prompt = injector.modalInput.getCurrentModalInput() ?: return
    prompt.deactivate(refocusOwningEditor = true, resetCaret = true)
  }
}