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
import com.maddyhome.idea.vim.key.VimKeyStroke

// TODO make it safer to utilise key.isCloseKeystroke and similar logic
abstract class VimInputInterceptorBase<T> : VimInputInterceptor {
  override fun consumeKey(
    key: VimKeyStroke,
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

  /**
   * Attempt to build a complete input from the given keystroke.
   *
   * @param key The current keystroke to process.
   * @return The complete input of type [T] if it can be constructed, or null if more keystrokes are needed.
   */
  protected abstract fun buildInput(key: VimKeyStroke): T?

  /**
   * Execute the action associated with the complete input.
   *
   * @param input The complete input of type T.
   * @param editor The active editor instance.
   * @param context The current execution context.
   */
  protected abstract fun executeInput(
    input: T,
    editor: VimEditor,
    context: ExecutionContext,
  )
}
