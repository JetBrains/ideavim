/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

public interface VimSelectionModel {
  public val selectionStart: Int
  public val selectionEnd: Int

  public fun hasSelection(): Boolean
}
