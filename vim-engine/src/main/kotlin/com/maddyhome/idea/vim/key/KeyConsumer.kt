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
import javax.swing.KeyStroke

internal interface KeyConsumer {
  /**
   * Returns true if this key consumer can attempt to process the key
   *
   * This is mainly a shortcut to aid debugging. Instead of stepping into each key consumer, it's possible to set a
   * breakpoint on the call to [consumeKey] instead.
   *
   * This function will always be called before [consumeKey].
   */
  fun isApplicable(
    key: KeyStroke,
    editor: VimEditor,
    allowKeyMappings: Boolean,
    keyProcessResultBuilder: KeyProcessResult.KeyProcessResultBuilder,
  ): Boolean

  /**
   * Attempt to process and consume the key, if applicable.
   *
   * This function will only be called if [isApplicable] returns `true` first.
   *
   * @return `true` if the key was consumed and no further [KeyConsumer] instances should be called
   */
  fun consumeKey(
    key: KeyStroke,
    editor: VimEditor,
    allowKeyMappings: Boolean,
    keyProcessResultBuilder: KeyProcessResult.KeyProcessResultBuilder,
  ): Boolean
}
