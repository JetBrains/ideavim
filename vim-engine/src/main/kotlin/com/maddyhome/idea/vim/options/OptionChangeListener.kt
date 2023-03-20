/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.options

import com.maddyhome.idea.vim.api.VimEditor

public fun interface OptionChangeListener<in T> {

  public fun processGlobalValueChange(oldValue: T?)
}

// options that can change their values in specific editors
public interface LocalOptionChangeListener<in T> : OptionChangeListener<T> {

  public fun processLocalValueChange(oldValue: T?, editor: VimEditor)
}
