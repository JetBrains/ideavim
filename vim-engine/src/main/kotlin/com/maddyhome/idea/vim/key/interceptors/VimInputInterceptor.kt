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
interface VimInputInterceptor<T> {
  /**
   * Process a single keystroke and attempt to build a complete input of type [T].
   */
  fun consumeKey(
    key: KeyStroke,
    editor: VimEditor,
    context: ExecutionContext,
  )

  /**
   * Attempt to build a complete input from the given keystroke.
   *
   * @param key The current keystroke to process.
   * @return The complete input of type [T] if it can be constructed, or null if more keystrokes are needed.
   */
  fun buildInput(key: KeyStroke): T?

  /**
   * Execute the action associated with the complete input.
   *
   * @param input The complete input of type T.
   * @param editor The active editor instance.
   * @param context The current execution context.
   */
  fun executeInput(
    input: T,
    editor: VimEditor,
    context: ExecutionContext,
  )
}