/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

interface VimMessages {
  fun showStatusBarMessage(editor: VimEditor?, message: String?)
  fun getStatusBarMessage(): String?
  fun clearStatusBarMessage()
  fun indicateError()
  fun clearError()
  fun isError(): Boolean
  fun message(key: String, vararg params: Any): String

  fun updateStatusBar(editor: VimEditor)
}
