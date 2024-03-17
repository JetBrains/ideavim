/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

public interface VimMessages {
  public fun showStatusBarMessage(editor: VimEditor?, message: String?)
  public fun getStatusBarMessage(): String?
  public fun indicateError()
  public fun clearError()
  public fun isError(): Boolean
  public fun message(key: String, vararg params: Any): String

  public fun updateStatusBar(editor: VimEditor)
}
