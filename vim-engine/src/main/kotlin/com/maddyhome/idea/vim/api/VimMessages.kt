/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.helper.EngineMessageHelper
import org.jetbrains.annotations.PropertyKey

interface VimMessages {
  fun showStatusBarMessage(editor: VimEditor?, message: String?)
  fun getStatusBarMessage(): String?
  fun clearStatusBarMessage()
  fun indicateError()
  fun clearError()
  fun isError(): Boolean

  /**
   * Fetch a message from the engine's resource bundle.
   *
   * Note that this will _only_ return messages from the engine's resource bundle. It will not return messages from
   * the host's resource bundle. Hosts should use an alternative method to fetch messages from their own resources.
   */
  fun message(@PropertyKey(resourceBundle = EngineMessageHelper.BUNDLE) key: String, vararg params: Any): String

  fun updateStatusBar(editor: VimEditor)
}
