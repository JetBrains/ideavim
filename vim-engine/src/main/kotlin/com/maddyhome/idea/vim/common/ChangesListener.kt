/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.common

public interface ChangesListener {
  public fun documentChanged(change: Change)

  public class Change(public val oldFragment: String, public val newFragment: String, public val offset: Int)
}
