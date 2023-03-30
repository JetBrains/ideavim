/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.helper

import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector

public object StrictMode {
  @JvmName("assertTrue")
  public fun assert(condition: Boolean, message: String) {
    if (!condition) {
      fail(message)
    }
  }

  public fun fail(message: String) {
    if (injector.globalOptions().ideastrictmode) {
      error(message)
    }
  }
}
