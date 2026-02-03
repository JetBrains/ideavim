/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.helper.EngineMessageHelper
import org.jetbrains.annotations.PropertyKey

interface VimMessages {
  /**
   * Displays an informational message to the user.
   * The message panel closes on any keystroke and passes the key through to the editor.
   */
  fun showMessage(editor: VimEditor, message: String?)

  /**
   * Displays an error message to the user (typically in red).
   * The message panel closes on any keystroke and passes the key through to the editor.
   */
  fun showErrorMessage(editor: VimEditor, message: String?)

  /**
   * Legacy method for displaying messages.
   * @deprecated Use [showMessage] or [showErrorMessage] instead.
   */
  @Deprecated("Use showMessage or showErrorMessage instead", ReplaceWith("showMessage(editor, message)"))
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
