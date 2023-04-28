/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.options

import com.maddyhome.idea.vim.api.VimEditor

/**
 * Listener for changes to the value of a global option
 *
 * This listener will only be called when a global option's value is changed. It is intended for non-editor related
 * options. That is, options that don't need to refresh editor(s) when the global value changes. For example, updating
 * the status bar widget when `'showcmd'` changes, or updating the default register when `'clipboard'` changes, or
 * enabling/disabling extensions.
 */
public fun interface GlobalOptionChangeListener {
  public fun onGlobalOptionChanged()
}

public fun interface OptionChangeListener<in T> {

  public fun processGlobalValueChange(oldValue: T?)
}

// options that can change their values in specific editors
public interface LocalOptionChangeListener<in T> : OptionChangeListener<T> {

  public fun processLocalValueChange(oldValue: T?, editor: VimEditor)
}
