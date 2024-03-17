/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.common.ChangesListener
import com.maddyhome.idea.vim.common.LiveRange
import com.maddyhome.idea.vim.common.Offset

public interface VimDocument {
  public fun addChangeListener(listener: ChangesListener)
  public fun removeChangeListener(listener: ChangesListener)
  public fun getOffsetGuard(offset: Offset): LiveRange?
}
