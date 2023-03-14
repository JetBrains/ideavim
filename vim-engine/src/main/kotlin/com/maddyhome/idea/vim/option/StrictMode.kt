/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.option

import com.maddyhome.idea.vim.api.Options
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector

@Deprecated("Please use StrictMode from com.maddyhome.idea.vim.options.helpers", replaceWith = ReplaceWith("com.maddyhome.idea.vim.helper.StrictMode"))
public object StrictMode {
  @JvmName("assertTrue")
  public fun assert(condition: Boolean, message: String) {
    if (!condition) {
      fail(message)
    }
  }

  public fun fail(message: String) {
    if (injector.globalOptions().isSet(Options.ideastrictmode)) {
      error(message)
    }
  }
}
