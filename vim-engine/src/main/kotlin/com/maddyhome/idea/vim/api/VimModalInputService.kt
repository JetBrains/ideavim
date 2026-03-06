/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.key.interceptors.VimInputInterceptor
import javax.swing.KeyStroke

interface VimModalInputService {
  fun getCurrentModalInput(): VimModalInput?
  fun create(
    editor: VimEditor,
    context: ExecutionContext,
    label: String,
    inputInterceptor: VimInputInterceptor,
  ): VimModalInput

  /**
   * Activates modal key input, processing each keystroke through the given [processor].
   * The processor returns `true` to continue accepting input, or `false` to stop.
   * Used by extensions to get single-character input from the user (e.g., `getchar()`).
   */
  fun activate(editor: VimEditor, processor: (KeyStroke) -> Boolean) {}
}
